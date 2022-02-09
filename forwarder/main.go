// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"context"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"
	"time"

	"cloud.google.com/go/bigtable"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"

	securityreport "github.com/GoogleCloudPlatform/reporting-api-processor/forwarder/proto"
)

const (
	tableName      = "security_report"
	columnFamily   = "description"
	btWriteBufSize = 1000
	interval       = 10 * time.Second
)

var (
	bt       *bigtable.Client
	reportCh chan *securityreport.SecurityReport
)

func init() {
	reportCh = make(chan *securityreport.SecurityReport, btWriteBufSize*10)
}

func main() {
	project := os.Getenv("BT_PROJECT")
	instance := os.Getenv("BT_INSTANCE")
	if project == "" || instance == "" {
		log.Fatalf("either of environment variables BT_PROJECT or BT_INSTANCE is empty.")
	}

	ctx := context.Background()
	var err error
	bt, err = bigtable.NewClient(ctx, project, instance)
	defer func() {
		if err := bt.Close(); err != nil {
			log.Fatalf("failed to close Cloud BigTable client: %v", err)
		}
	}()
	if err != nil {
		log.Fatalf("failed to create BigTable client: %v", err)
	}

	go reportWriter(ctx, reportCh)

	e := echo.New()
	e.Debug = true
	e.HideBanner = true
	e.HidePort = true
	// In order to handle Reporting API, the reporting endpoint needs to handle CORS
	e.Use(middleware.CORSWithConfig(middleware.DefaultCORSConfig))
	e.POST("/main", mainHandler)
	e.POST("/default", mainHandler)
	e.GET("/_healthz", healthzHandler)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	if err := e.Start(":" + port); err != nil {
		log.Fatalf("failure occured on launching HTTP server: %v", err)
	}
}

func healthzHandler(c echo.Context) error {
	return c.String(http.StatusOK, "OK")
}

func mainHandler(c echo.Context) error {
	r := c.Request()
	contentType := r.Header.Get("Content-Type")
	if contentType != "application/reports+json" {
		c.Echo().Logger.Errorf("Content-Type header is not application/reports+json: %v", r.Header)
		return c.String(http.StatusBadRequest, "Content-Type not supported. The Content-Type must be application/reports+json.")
	}

	data, err := io.ReadAll(r.Body)
	if err != nil {
		c.Echo().Logger.Errorf("error on reading data: %v", err)
	}
	c.Echo().Logger.Debug(string(data))
	if err := r.Body.Close(); err != nil {
		return err
	}

	var buf []map[string]interface{}
	err = json.Unmarshal(data, &buf)
	if err != nil {
		c.Echo().Logger.Errorf("error on parsing JSON: %v", err)
		return c.String(http.StatusInternalServerError, err.Error())
	}
	c.Echo().Logger.Debugf("queueing %v reports", len(buf))
	for _, b := range buf {
		// TODO(yoshifumi): Extrace values with keys and store them into SecurityReport
		reportCh <- mapToSecurityReport(c.Logger(), b)
	}

	return c.String(http.StatusOK, "OK")
}

func reportWriter(ctx context.Context, ch chan *securityreport.SecurityReport) {
	t := time.NewTicker(interval)
	table := bt.Open(tableName)
	defer t.Stop()
	for {
		select {
		case <-t.C:
			size := min(len(ch), btWriteBufSize)
			// log.Printf("buffered %v reports", size)
			if size == 0 {
				// log.Printf("no reports buffered. skipping.")
				continue
			}
			muts := make([]*bigtable.Mutation, size)
			keys := make([]string, size)
			for i := 0; i < size; i++ {
				muts[i] = bigtable.NewMutation()
				r := <-ch
				setSecurityReport(muts[i], r)
				keys[i] = generateRowKey(r)
			}
			rowErrs, err := table.ApplyBulk(ctx, keys, muts)
			if err != nil {
				log.Printf("could not apply bulk row mutation: %v", err)
			}
			for _, rowErr := range rowErrs {
				log.Printf("error writing row: %v", rowErr)
			}
			log.Printf("wrote %v reports", size)
		}
	}
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
