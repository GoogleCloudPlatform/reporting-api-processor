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
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"

	securityreport "github.com/GoogleCloudPlatform/reporting-api-processor/forwarder/proto"
)

func main() {
	e := echo.New()
	e.HideBanner = true
	e.HidePort = true
	// In order to handle Reporting API, the reporting endpoint needs to handle CORS
	e.Use(middleware.CORSWithConfig(middleware.DefaultCORSConfig))
	e.POST("/main", mainHandler)
	e.GET("/healthz", healthzHandler)

	if err := e.Start(":3000"); err != nil {
		logger.Fatal().Msgf("failure occured on launching HTTP server: %v", err)
	}
}

func healthzHandler(c echo.Context) error {
	return c.String(http.StatusOK, fmt.Sprintf("OK\n%v", time.Now()))
}

func mainHandler(c echo.Context) error {
	r := c.Request()
	contentType := r.Header.Get("Content-Type")
	if contentType != "application/reports+json" {
		logger.Error().Msgf("Content-Type header is not application/reports+json: %v", r.Header)
		return c.String(http.StatusBadRequest, "Content-Type not supported. The Content-Type must be application/reports+json.")
	}

	data, err := io.ReadAll(r.Body)
	if err != nil {
		logger.Error().Msgf("error on reading data: %v", err)
	}
	if err := r.Body.Close(); err != nil {
		return err
	}

	var buf []report
	err = json.Unmarshal(data, &buf)
	if err != nil {
		logger.Error().Msgf("error on parsing JSON: %v", err)
		return c.String(http.StatusInternalServerError, err.Error())
	}
	for _, b := range buf {
		// TODO(yoshifumi): Extrace values with keys and store them into SecurityReport
		_ = b
	}

	sr := securityreport.SecurityReport{}
	_ = sr
	return c.String(http.StatusOK, "OK")
}
