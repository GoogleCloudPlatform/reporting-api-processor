package com.google.collector.clustering;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import reports.CspReportOuterClass.CspReport;
import reports.DeprecationReportOuterClass.DeprecationReport;
import reports.SecurityReportOuterClass.SecurityReport;

class ExtractClusterKey extends DoFn<SecurityReport, KV<String, SecurityReport>> {

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
