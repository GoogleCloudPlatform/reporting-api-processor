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
      return;
    }

    ctx.output(KV.of(key, securityReport));
  }

  private String buildClusterKeyForCsp(CspReport cspReport) {
    // inline and eval are grouped by script sample, otherwise group by blocked URI
    return cspReport.getBlockedUri().equals("inline") || cspReport.getBlockedUri().equals("eval")
        ? cspReport.getScriptSample()
        : cspReport.getBlockedUri();
  }
}
