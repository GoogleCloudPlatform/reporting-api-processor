package com.google.collector.loaders;

import static reports.SecurityReportOuterClass.*;

import com.google.common.collect.ImmutableList;
import java.util.Scanner;
import org.apache.beam.sdk.transforms.DoFn;
import reports.CspReportOuterClass.CspReport;
import reports.SecurityReportOuterClass.SecurityReport.Disposition;

// Prototype CSP reports are exported as
// 0: report_id
// 1: report_time
// 2: domain
// 3: document_uri
// 4: referrer
// 5: blocked_uri
// 6: violated_directive
// 7: original_policy
// 8: source_file
// 9: csp_version
// 10: user_agent
// 11: product_name
// 12: version
// 13: effective_directive
// 14: browser
// 15: violation_candidates
// 16: original_report_uris
// 17: metadata
// 18: count
// 19: script_sample
// 20: disposition
// 21: blocked_uri_domain
// 22: noise_info
// 23: policy_with_strict_dynamic
// 24: blocked_uri_domain_google_owned
// 25: policy_type
// 26: policy_with_unsafe_eval
// 27: source_file_domain
// 28: source_file_domain_google_owned
// 29: line_number
// 30: column_number
class LoadFromCsv extends DoFn<String, SecurityReport> {

  @ProcessElement
  public void processElement(ProcessContext ctx) throws IllegalArgumentException {
    ImmutableList<String> columns = loadColumns(ctx);
    // hacky skip of unwanted lines: header, empties, unparsed
    if (columns.isEmpty() || columns.size() <= 12 || columns.get(1).equals("report_time")) {
      return;
    }

    CspReport cspReport = CspReport.newBuilder()
        .setReferrer(columns.get(4))
        .setBlockedUri(columns.get(5))
        .setViolatedDirective(columns.get(6))
        .setOriginalPolicy(columns.get(7))
        .setSourceFile(columns.get(8))
        .setEffectiveDirective(columns.get(13))
        .setScriptSample(columns.get(19))
        .setLineNumber(-1)
        .setColumnNumber(-1)
        .build();

    SecurityReport securityReport = SecurityReport.newBuilder()
        .setReportChecksum("foo")
        .setReportTime(1L)
        .setReportCount(1L)
        .setUserAgent("foo")
        .setDisposition(Disposition.REPORTING)
        .setCspReport(cspReport)
        .build();
    ctx.output(securityReport);
  }

  private ImmutableList<String> loadColumns(ProcessContext ctx) {
    ImmutableList.Builder<String> values = ImmutableList.builder();
    try (Scanner rowScanner = new Scanner(
        ctx.element().replaceAll("\n", "")
    )) {
      rowScanner.useDelimiter(",");
      while (rowScanner.hasNext()) {
        values.add(rowScanner.next());
      }
    }

    return values.build();
  }
}