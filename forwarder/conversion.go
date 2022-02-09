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
	"strconv"

	"cloud.google.com/go/bigtable"
	securityreport "github.com/GoogleCloudPlatform/reporting-api-processor/forwarder/proto"
	"github.com/labstack/echo/v4"
)

func mapToSecurityReport(logger echo.Logger, m map[string]interface{}) *securityreport.SecurityReport {
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
		if ln, ok := body["lineNumber"].(int32); ok {
			csp.LineNumber = ln
		} else {
			logger.Warnf("unexpected lineNumber: %#v", body["lineNumber"])
		}
		if cn, ok := body["columnNumber"].(int32); ok {
			csp.ColumnNumber = cn
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
			logger.Warnf("unexpected anticipatedReval: %#v", body["anticipatedRemoval"])
		}
		if ln, ok := body["lineNumber"].(int32); ok {
			dep.LineNumber = ln
		} else {
			logger.Warnf("unexpected lineNumber: %#v", body["lineNumber"])
		}
		if cn, ok := body["columnNumber"].(int32); ok {
			dep.ColumnNumber = cn
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
