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

package com.google.collector;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation.Required;

/**
 * Command-line arguments to the Collector pipeline.
 */
public interface CollectorOptions extends PipelineOptions {

  @Description("Whether to load reports from BigTable")
  @Default.Boolean(true)
  boolean getLoadFromBigTable();

  void setLoadFromBigTable(boolean value);

  @Description("Whether to output reports to BigTable")
  @Default.Boolean(false)
  boolean getOutputToBigTable();

  void setOutputToBigTable(boolean value);

  @Description("Path of the file to read from")
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
