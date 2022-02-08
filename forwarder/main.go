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
	"net/http"
	"os"
	"strconv"
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
		logger.Fatal().Msgf("either of environment variables BT_PROJECT or BT_INSTANCE is empty.")
	}

	ctx := context.Background()
	var err error
	bt, err = bigtable.NewClient(ctx, project, instance)
	defer func() {
		if err := bt.Close(); err != nil {
			logger.Fatal().Msgf("failed to close Cloud BigTable client: %v", err)
		}
	}()
	if err != nil {
		logger.Fatal().Msgf("failed to create BigTable client: %v", err)
	}

	go reportWriter(ctx, reportCh)

	e := echo.New()
	e.HideBanner = true
	e.HidePort = true
	// In order to handle Reporting API, the reporting endpoint needs to handle CORS
	e.Use(middleware.CORSWithConfig(middleware.DefaultCORSConfig))
	e.POST("/main", mainHandler)
	e.GET("/_healthz", healthzHandler)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	if err := e.Start(":" + port); err != nil {
		logger.Fatal().Msgf("failure occured on launching HTTP server: %v", err)
	}
}

func healthzHandler(c echo.Context) error {
	return c.String(http.StatusOK, "OK")
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

	var buf []map[string]interface{}
	err = json.Unmarshal(data, &buf)
	if err != nil {
		logger.Error().Msgf("error on parsing JSON: %v", err)
		return c.String(http.StatusInternalServerError, err.Error())
	}
	for _, b := range buf {
		// TODO(yoshifumi): Extrace values with keys and store them into SecurityReport
		reportCh <- mapToSecurityReport(b)
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
			logger.Debug().Msgf("buffered %v reports", size)
			if size == 0 {
				logger.Debug().Msgf("no reports buffered. skipping.")
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
				logger.Error().Msgf("could not apply bulk row mutation: %v", err)
			}
			for _, rowErr := range rowErrs {
				logger.Error().Msgf("error writing row: %v", rowErr)
			}
			logger.Info().Msgf("wrote %v reports", size)
		}
	}
}

func mapToSecurityReport(m map[string]interface{}) *securityreport.SecurityReport {
	sr := &securityreport.SecurityReport{}

	sr.ReportChecksum = strconv.Itoa(0)
	sr.ReportCount = int64(1)
	sr.Disposition = securityreport.SecurityReport_DISPOSITION_UNKNOWN
	if rt, ok := m["report_time"].(int64); ok {
		sr.ReportTime = rt
	}
	if ua, ok := m["user_agent"].(string); ok {
		sr.UserAgent = ua
	}
	var typ string
	var body map[string]interface{}
	var ok bool
	if typ, ok = m["type"].(string); !ok {
		logger.Error().Msgf("unexpected report type: %v", m)
		return sr
	}
	if body, ok = m["body"].(map[string]interface{}); !ok {
		logger.Error().Msgf("unexpected report type: %v", m)
		return sr
	}
	switch typ {
	case "csp-violation":
		csp := &securityreport.CspReport{}
		csp.DocumentUri = body["documentURL"].(string)
		csp.Referrer = body["referrer"].(string)
		csp.BlockedUri = body["blockedURL"].(string)
		csp.ViolatedDirective = body["violatedDirective"].(string)
		csp.EffectiveDirective = body["effectiveDirective"].(string)
		csp.SourceFile = body["sourceFile"].(string)
		csp.LineNumber = body["lineNumber"].(int32)
		csp.ColumnNumber = body["columnNumber"].(int32)
		csp.ScriptSample = body["scriptSample"].(string)
		sr.ReportExtension = &securityreport.SecurityReport_CspReport{CspReport: csp}

		switch body["disposition"].(string) {
		case "enforce":
			sr.Disposition = securityreport.SecurityReport_ENFORCED
		case "report":
			sr.Disposition = securityreport.SecurityReport_REPORTING
		default:
		}
	case "deprecation":
		dep := &securityreport.DeprecationReport{}
		dep.Id = body["id"].(string)
		dep.AnticipatedRemoval = body["anticipatedRemoval"].(string)
		dep.LineNumber = body["lineNumber"].(int32)
		dep.ColumnNumber = body["columnNumber"].(int32)
		dep.Message = body["message"].(string)
		dep.SourceFile = body["sourceFile"].(string)
		sr.ReportExtension = &securityreport.SecurityReport_DeprecationReport{DeprecationReport: dep}
	}
	return sr
}

func generateRowKey(r *securityreport.SecurityReport) string {
	ext := "csp"
	if r.GetDeprecationReport() != nil {
		ext = "dep"
	}
	// row key format: report_extension#checksum#timestamp
	return ext + "#" + r.GetReportChecksum() + "#" + strconv.FormatInt(r.GetReportTime(), 10)
}

func setSecurityReport(m *bigtable.Mutation, r *securityreport.SecurityReport) {
	now := bigtable.Now()
	m.Set(columnFamily, "report_checksum", now, []byte(strconv.Itoa(0))) // report_checksum is 0 before aggregation
	m.Set(columnFamily, "report_count", now, []byte(strconv.Itoa(1)))    // report_count must be 1 before aggregation
	m.Set(columnFamily, "user_agent", now, []byte(r.GetUserAgent()))
	m.Set(columnFamily, "browser_name", now, []byte(r.GetBrowserName()))
	m.Set(columnFamily, "browser_major_version", now, []byte(strconv.FormatInt(int64(r.GetBrowserMajorVersion()), 10)))
	m.Set(columnFamily, "disposition", now, []byte(r.GetDisposition().String()))
	if csp := r.GetCspReport(); csp != nil {
		m.Set(columnFamily, "document_uri", now, []byte(csp.GetDocumentUri()))
		m.Set(columnFamily, "referrer", now, []byte(csp.GetReferrer()))
		m.Set(columnFamily, "blocked_uri", now, []byte(csp.GetBlockedUri()))
		m.Set(columnFamily, "violated_directive", now, []byte(csp.GetViolatedDirective()))
		m.Set(columnFamily, "effective_directive", now, []byte(csp.GetEffectiveDirective()))
		m.Set(columnFamily, "original_policy", now, []byte(csp.GetOriginalPolicy()))
		m.Set(columnFamily, "source_file", now, []byte(csp.GetSourceFile()))
		m.Set(columnFamily, "line_number", now, []byte(strconv.FormatInt(int64(csp.GetLineNumber()), 10)))
		m.Set(columnFamily, "column_number", now, []byte(strconv.FormatInt(int64(csp.GetColumnNumber()), 10)))
		m.Set(columnFamily, "script_sample", now, []byte(csp.GetScriptSample()))
	}
	if dep := r.GetDeprecationReport(); dep != nil {
		m.Set(columnFamily, "id", now, []byte(dep.GetId()))
		m.Set(columnFamily, "anticipated_removal", now, []byte(dep.GetAnticipatedRemoval()))
		m.Set(columnFamily, "message", now, []byte(dep.GetMessage()))
		m.Set(columnFamily, "source_file", now, []byte(dep.GetSourceFile()))
		m.Set(columnFamily, "line_number", now, []byte(strconv.FormatInt(int64(dep.GetLineNumber()), 10)))
		m.Set(columnFamily, "column_number", now, []byte(strconv.FormatInt(int64(dep.GetColumnNumber()), 10)))
	}
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
