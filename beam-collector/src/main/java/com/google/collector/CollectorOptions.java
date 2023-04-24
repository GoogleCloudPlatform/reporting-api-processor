package com.google.collector;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation.Required;

public interface CollectorOptions extends PipelineOptions {

  @Description("Whether to load reports from BigTable")
  @Default.Boolean(false)
  boolean getLoadFromBigTable();

  void setLoadFromBigTable(boolean value);

  @Description("Whether to output reports to BigTable")
  @Default.Boolean(false)
  boolean getOutputToBigTable();

  void setOutputToBigTable(boolean value);

  @Description("Path of the file to read from")
  @Default.String("gs://stargazing-collector-bucket/sample-csp-reports.csv")
  String getInputFile();

  void setInputFile(String value);

  @Description("Path of the file to write to")
  @Required
  String getOutput();

  void setOutput(String value);

  @Description("The Bigtable project ID, this can be different than your Dataflow project")
  @Default.String("bigtable-project")
  String getBigtableProjectId();

  void setBigtableProjectId(String bigtableProjectId);

  @Description("The Bigtable instance ID")
  @Default.String("bigtable-instance")
  String getBigtableInstanceId();

  void setBigtableInstanceId(String bigtableInstanceId);

  @Description("The Bigtable table ID in the instance.")
  @Default.String("bigtable-table")
  String getBigtableTableId();

  void setBigtableTableId(String bigtableTableId);
}
