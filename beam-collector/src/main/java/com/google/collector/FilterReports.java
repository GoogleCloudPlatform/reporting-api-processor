package com.google.collector;

import org.apache.beam.sdk.transforms.SerializableFunction;
import reports.SecurityReportOuterClass.SecurityReport;

/**
 * Generic filter transform defaults to processing all incoming reports. This class should be updated
 * with custom logic to exclude any reports that should be excluded from being clustered.
 */
public class FilterReports implements SerializableFunction<SecurityReport, Boolean> {

  @Override
  public Boolean apply(SecurityReport input) {
    return true;
  }
}
