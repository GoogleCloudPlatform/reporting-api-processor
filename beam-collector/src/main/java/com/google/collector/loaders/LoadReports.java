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

package com.google.collector.loaders;

import com.google.bigtable.v2.Cell;
import com.google.bigtable.v2.Family;
import com.google.bigtable.v2.Row;
import com.google.collector.CollectorOptions;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import java.util.Optional;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigtable.BigtableIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import reports.SecurityReportOuterClass.SecurityReport;

/**
 * Factory class used to load pre-defined transforms that can read raw Reporting API reports from
 * various data sources.
 */
public final class LoadReports {

  private LoadReports() {}

  public static PCollection<SecurityReport> fromCsv(Pipeline p, String inputFile) {
    return p.apply("LoadFromCsv", TextIO.read().from(inputFile))
        .apply("CsvToReports", ParDo.of(new LoadFromCsv()));
  }

  public static PCollection<SecurityReport> fromBigTable(Pipeline p, CollectorOptions options) {
    // scans entire table
    return p.apply("LoadFromBigTable", BigtableIO.read()
        .withProjectId(options.getBigtableProjectId())
        .withInstanceId(options.getBigtableInstanceId())
        .withTableId(options.getBigtableTableId()))
        .apply("RowsToReports", ParDo.of(new DoFn<Row, SecurityReport>() {
          @ProcessElement
          public void processElement(ProcessContext c) {
            Optional<byte[]> bytes = c.element().getFamiliesList().stream()
                // find target family
                .filter(f -> "description".equals(f.getName()))
                .findFirst()
                // find target column
                .map(Family::getColumnsList)
                .flatMap(columns -> columns.stream()
                    .filter(col -> "data".equals(col.getQualifier().toStringUtf8())).findFirst()
                    .map(col -> col.getCells(0)))
                // extract report as byte array
                .map(Cell::getValue)
                .map(ByteString::toByteArray);

            if (!bytes.isPresent()) {
              // ignore malformed reports
              return;
            }

            try {
              Parser<SecurityReport> reportParser = SecurityReport.getDefaultInstance().getParserForType();
              SecurityReport securityReport = reportParser.parseFrom(bytes.get());
              c.output(securityReport);
            } catch (InvalidProtocolBufferException ignored) {
              // ignore malformed reports
            }
          }
        }));
  }
}
