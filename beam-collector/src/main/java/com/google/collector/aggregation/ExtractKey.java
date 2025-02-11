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

package com.google.collector.aggregation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import reports.CspReportOuterClass.CspReport;
import reports.DeprecationReportOuterClass.DeprecationReport;
import reports.SecurityReportOuterClass.SecurityReport;

/**
 * Uses the adler32 algorithm to product per-violation checksums.
 */
final class ExtractKey extends DoFn<SecurityReport, KV<Long, SecurityReport>> {

  private static final Pattern NONCE_PATTERN = Pattern.compile("'nonce-.+?'");

  @ProcessElement
  public void processElement(ProcessContext ctx) {
    SecurityReport report = ctx.element();
    if (report == null) {
      return;
    }

    long checksum = 0;
    switch (report.getReportExtensionCase()) {
      case CSP_REPORT:
        checksum = computeForCsp(report.getCspReport());
        break;
      case DEPRECATION_REPORT:
        checksum = computeForDeprecation(report.getDeprecationReport());
        break;
    }

    ctx.output(KV.of(checksum, report));
  }

  private long computeForDeprecation(DeprecationReport deprecationReport) {
    Adler32 adler32 = new Adler32();
    adler32.update(deprecationReport.getIdBytes().toByteArray());
    adler32.update(deprecationReport.getSourceFileBytes().toByteArray());
    adler32.update(deprecationReport.getLineNumber());
    adler32.update(deprecationReport.getColumnNumber());

    return adler32.getValue();
  }

  private long computeForCsp(CspReport report) {
    // TODO: include parsed UserAgent
    Adler32 adler32 = new Adler32();
    adler32.update(report.getDocumentUriBytes().toByteArray());
    adler32.update(report.getBlockedUriBytes().toByteArray());
    adler32.update(normalizeNonces(report.getViolatedDirective()));
    adler32.update(report.getEffectiveDirectiveBytes().toByteArray());
    adler32.update(normalizeNonces(report.getOriginalPolicy()));
    adler32.update(report.getSourceFileBytes().toByteArray());
    adler32.update(report.getDocumentUriBytes().toByteArray());
    adler32.update(report.getLineNumber());
    adler32.update(report.getColumnNumber());

    return adler32.getValue();
  }

  private byte[] normalizeNonces(String violatedDirective) {
    Matcher matcher = NONCE_PATTERN.matcher(violatedDirective);
    return matcher.replaceAll("'nonce-REDACTED'").getBytes();
  }
}
