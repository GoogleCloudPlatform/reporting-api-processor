package com.google.collector;

import org.apache.beam.sdk.transforms.SerializableFunction;
import reports.SecurityReportOuterClass.SecurityReport;

public class FilterReports implements SerializableFunction<SecurityReport, Boolean> {

  @Override
  public Boolean apply(SecurityReport input) {
    return true;
  }
}
