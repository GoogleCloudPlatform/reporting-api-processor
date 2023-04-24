/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import com.google.collector.aggregation.Aggregate;
import com.google.collector.clustering.Cluster;
import com.google.collector.loaders.LoadReports;
import com.google.collector.persistence.Persist;
import com.google.collector.persistence.PersistenceTransform;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.values.PCollection;
import reports.SecurityReportOuterClass.SecurityReport;

public class Collector {

  public static void main(String[] args) {
    CollectorOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CollectorOptions.class);

    Pipeline p = Pipeline.create(options);

    PCollection<SecurityReport> reports = options.getLoadFromBigTable()
            ? LoadReports.fromBigTable(p, options)
            : LoadReports.fromCsv(p, options.getInputFile());

    PersistenceTransform persistenceTransform;
    if (options.getOutputToBigTable()) {
      CloudBigtableTableConfiguration bigtableTableConfig =
          new CloudBigtableTableConfiguration.Builder()
              .withProjectId(options.getBigtableProjectId())
              .withInstanceId(options.getBigtableInstanceId())
              .withTableId(options.getBigtableTableId())
              .build();

      persistenceTransform = Persist.toBigTable(bigtableTableConfig);
    } else {
      persistenceTransform = Persist.toJson(options.getOutput());
    }

    reports
        .apply("Filter", Filter.by(new FilterReports()))
        .apply("Aggregate", new Aggregate())
        .apply("Cluster", new Cluster())
        .apply("Output", persistenceTransform);

    p.run().waitUntilFinish();
  }
}
