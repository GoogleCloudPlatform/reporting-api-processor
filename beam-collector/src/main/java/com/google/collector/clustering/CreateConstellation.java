package com.google.collector.clustering;

import com.google.common.collect.ImmutableSet;
import constellations.ConstellationOuterClass.Constellation;
import constellations.ConstellationOuterClass.Constellation.Feature;
import constellations.ConstellationOuterClass.Constellation.Refactoring;
import constellations.ConstellationOuterClass.ConstellationMetadata;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import reports.CspReportOuterClass.CspReport;
import reports.SecurityReportOuterClass.SecurityReport;

/**
 * This transform aggregates reports that share the same root cause and computes useful metrics that
 * can help determine whether this root cause is actionable.
 */
class CreateConstellation extends DoFn<KV<String, Iterable<SecurityReport>>, Constellation> {

  @ProcessElement
  public void processElement(ProcessContext ctx) {
    KV<String, Iterable<SecurityReport>> element = ctx.element();
    if (element == null) {
      return;
    }

    Iterable<SecurityReport> reports = element.getValue();
    if (reports == null || !reports.iterator().hasNext()) {
      return;
    }

    SecurityReport sample = reports.iterator().next();
    long totalCount = 0;
    ImmutableSet.Builder<String> checksums = ImmutableSet.builder();
    for (SecurityReport securityReport : reports) {
      // always pick the latest report
      if (securityReport.getReportTime() > sample.getReportTime()) {
        sample = securityReport;
      }

      totalCount += securityReport.getReportCount();
      checksums.add(securityReport.getReportChecksum());
    }

    ConstellationMetadata metadata = ConstellationMetadata.newBuilder()
        .setReportCount(totalCount)
        .setLatestSeenTimestamp(sample.getReportTime())
        .setName(element.getKey())
        .setSample(sample)
        .addAllReports(checksums.build())
        .build();

    ctx.output(Constellation.newBuilder()
        .setId(element.getKey())
        .setFeature(determineFeature(sample))
        .setRefactoring(determineRefactoring(sample))
        .setMetadata(metadata)
        .build());
  }

  private Refactoring determineRefactoring(SecurityReport sample) {
    switch (sample.getReportExtensionCase()) {
      case CSP_REPORT:
        CspReport cspReport = sample.getCspReport();
        if (cspReport.getBlockedUri().equals("eval")) {
          return Refactoring.CSP_EVAL;
        } else if (cspReport.getBlockedUri().equals("inline")) {
          return Refactoring.CSP_INLINE;
        } else if (cspReport.getEffectiveDirective().equals("frame-ancestors") || cspReport.getEffectiveDirective().equals("frame-src")) {
          return Refactoring.CSP_FRAME_ANCESTORS;
        } else if (cspReport.getEffectiveDirective().startsWith("style-src")) {
          return Refactoring.CSP_STYLE;
        } else if (cspReport.getEffectiveDirective().startsWith("img-src")) {
          return Refactoring.CSP_MEDIA;
        } else if (cspReport.getEffectiveDirective().equals("require-trusted-types-for")) {
          return Refactoring.TRUSTED_TYPES_SINK;
        } else {
          return Refactoring.CSP_URL;
        }
      case DEPRECATION_REPORT:
        return Refactoring.API_REPLACEMENT;
      default:
        return Refactoring.REFACTORING_UNKNOWN;
    }
  }

  private Feature determineFeature(SecurityReport sample) {
    switch (sample.getReportExtensionCase()) {
      case CSP_REPORT:
        return Feature.CSP;
      case DEPRECATION_REPORT:
        return Feature.DEPRECATION;
      default:
        return Feature.FEATURE_UNKNOWN;
    }
  }
}
