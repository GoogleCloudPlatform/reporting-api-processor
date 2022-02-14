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
	"crypto/sha256"
	"encoding/json"
	"strconv"
	"time"

	"cloud.google.com/go/bigtable"
	securityreport "github.com/GoogleCloudPlatform/reporting-api-processor/forwarder/proto"
	"github.com/labstack/echo/v4"
)

const (
	tableName    = "security_report"
	columnFamily = "description"
	column       = "data"
)

func mapToSecurityReport(logger echo.Logger, m map[string]interface{}) *securityreport.SecurityReport {
	sr := &securityreport.SecurityReport{}
	now := time.Now().UnixMilli()
	deserialized, err := json.Marshal(m)
	if err != nil {
		logger.Errorf("failed to marshal map[string]interface{}: %v", m)
	}
	checksum := sha256.Sum256(deserialized)
	sr.ReportChecksum = string(checksum[:])
	sr.ReportCount = int64(1)
	sr.Disposition = securityreport.SecurityReport_DISPOSITION_UNKNOWN

	// the report has "age" field that is the offset from the report's timestamp.
	// https://w3c.github.io/reporting/#serialize-reports
	//
	// NOTE: currently the report doesn't have "timestamp" field, so use server side
	// current time.
	if age, ok := m["age"].(float64); ok {
		sr.ReportTime = now - int64(age)
	}
	if ua, ok := m["user_agent"].(string); ok {
		sr.UserAgent = ua
	}
	var typ string
	var body map[string]interface{}
	var ok bool
	if typ, ok = m["type"].(string); !ok {
		logger.Errorf("unexpected report type: %v", m)
		return sr
	}
	if body, ok = m["body"].(map[string]interface{}); !ok {
		logger.Errorf("unexpected report type: %v", m)
		return sr
	}
	switch typ {
	case "csp-violation":
		csp := &securityreport.CspReport{}
		if duri, ok := body["documentURL"].(string); ok {
			csp.DocumentUri = duri
		} else {
			logger.Warnf("unexpected documentURL: %#v", body["documentURL"])
		}
		if ref, ok := body["referrer"].(string); ok {
			csp.Referrer = ref
		} else {
			logger.Warnf("unexpected referrer: %#v", body["referrer"])
		}
		if buri, ok := body["blockedURL"].(string); ok {
			csp.BlockedUri = buri
		} else {
			logger.Warnf("unexpected blockedURL: %#v", body["blockedURL"])
		}
		if vd, ok := body["violatedDirective"].(string); ok {
			csp.ViolatedDirective = vd
		} else {
			logger.Warnf("unexpected violatedDirective: %#v", body["violatedDirective"])
		}
		if ed, ok := body["effectiveDirective"].(string); ok {
			csp.EffectiveDirective = ed
		} else {
			logger.Warnf("unexpected effectiveDirective: %#v", body["effectiveDirective"])
		}
		if sf, ok := body["sourceFile"].(string); ok {
			csp.SourceFile = sf
		} else {
			logger.Warnf("unexpected sourceFile: %#v", body["sourceFile"])
		}
		if ln, ok := body["lineNumber"].(float64); ok {
			csp.LineNumber = int32(ln)
		} else {
			logger.Warnf("unexpected lineNumber: %#v", body["lineNumber"])
		}
		if cn, ok := body["columnNumber"].(float64); ok {
			csp.ColumnNumber = int32(cn)
		} else {
			logger.Warnf("unexpected columnNumber: %#v", body["columnNumber"])
		}
		if ss, ok := body["scriptSample"].(string); ok {
			csp.ScriptSample = ss
		} else {
			logger.Warnf("unexpected scriptSample: %#v", body["scriptSample"])
		}
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
		if id, ok := body["id"].(string); ok {
			dep.Id = id
		} else {
			logger.Warnf("unexpected id: %#v", body["id"])
		}
		if ar, ok := body["anticipatedRemoval"].(string); ok {
			dep.AnticipatedRemoval = ar
		} else {
			logger.Warnf("unexpected anticipatedRemoval: %#v", body["anticipatedRemoval"])
		}
		if ln, ok := body["lineNumber"].(float64); ok {
			dep.LineNumber = int32(ln)
		} else {
			logger.Warnf("unexpected lineNumber: %#v", body["lineNumber"])
		}
		if cn, ok := body["columnNumber"].(float64); ok {
			dep.ColumnNumber = int32(cn)
		} else {
			logger.Warnf("unexpected columnNumber: %#v", body["columnNumber"])
		}
		if m, ok := body["message"].(string); ok {
			dep.Message = m
		} else {
			logger.Warnf("unexpected message: %#v", body["message"])
		}
		if sf, ok := body["sourceFile"].(string); ok {
			dep.SourceFile = sf
		} else {
			logger.Warnf("unexpected sourceFile: %#v", body["sourceFile"])
		}
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
	serialized := []byte(r.String())
	m.Set(columnFamily, column, now, serialized)
}
