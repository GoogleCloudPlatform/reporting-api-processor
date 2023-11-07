/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.collector.clustering;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import reports.CspReportOuterClass.CspReport;
import reports.DeprecationReportOuterClass.DeprecationReport;
import reports.SecurityReportOuterClass.SecurityReport;

/**
 * Transform used to build cluster keys from individual reports. Cluster keys are built from key
 * report fields. The set of fields used to create a given key depends on the type of report.
 * Whenever two or more reports share the same cluster key, they will be grouped together by this
 * pipeline.
 */
final class ExtractClusterKey extends DoFn<SecurityReport, KV<String, SecurityReport>> {

  @ProcessElement
  public void processElement(ProcessContext ctx) {
    SecurityReport securityReport = ctx.element();
    if (securityReport == null) {
      return;
    }

    String key = null;
    switch (securityReport.getReportExtensionCase()) {
      case CSP_REPORT:
        key = buildClusterKeyForCsp(securityReport.getCspReport());
        break;
      case DEPRECATION_REPORT:
        DeprecationReport deprecationReport = securityReport.getDeprecationReport();
        String id = deprecationReport.getId();
        key = id.isBlank() || id.isEmpty() ? "blankDeprecationId" : id;
        break;
    }

    if (key == null) {
      // TODO: a new report type that we don't support has been found - report it
      return;
    }

    ctx.output(KV.of(key, securityReport));
  }

  private String buildClusterKeyForCsp(CspReport cspReport) {
    // inline and eval are grouped by script sample, otherwise group by blocked URI
    if (cspReport.getBlockedUri().equals("inline") || cspReport.getBlockedUri().equals("eval")) {
      String scriptSample = cspReport.getScriptSample();
      return scriptSample.isBlank() || scriptSample.isEmpty() ? cspReport.getDocumentUri() : scriptSample;
    }

    // if there is not blocked-uri,
    return cspReport.getBlockedUri();
  }
}
