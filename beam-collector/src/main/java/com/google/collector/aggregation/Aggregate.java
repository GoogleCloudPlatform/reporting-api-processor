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

import java.util.concurrent.atomic.AtomicLong;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import reports.SecurityReportOuterClass.SecurityReport;

/**
 * Transform used to aggregate and de-duplicate reports together. This class builds a unique
 * identifier from individual reports. Reports that share the same identifier are likely to be
 * repeated violations of the same root cause and can be safely discarded.
 */
public class Aggregate extends PTransform<PCollection<SecurityReport>, PCollection<SecurityReport>> {

  @Override
  public PCollection<SecurityReport> expand(PCollection<SecurityReport> input) {
    // compute an identifier for every report
    return input.apply("ExtractReportKey", ParDo.of(new ExtractKey()))
        // find reports with colliding identifiers - they correspond to the same root cause
        .apply("GroupByReportKey", GroupByKey.create())
        // reduce them into a single report representing all of them
        .apply("Aggregate", ParDo.of(new DeDuplicate()));
  }

  static class DeDuplicate extends DoFn<KV<Long, Iterable<SecurityReport>>, SecurityReport> {

    @ProcessElement
    public void processElement(ProcessContext ctx) {
      KV<Long, Iterable<SecurityReport>> element = ctx.element();
      if (element == null) {
        return;
      }

      Iterable<SecurityReport> reports = element.getValue();
      if (reports == null || !reports.iterator().hasNext()) {
        return;
      }

      SecurityReport.Builder aggregate = reports.iterator().next().toBuilder();

      AtomicLong count = new AtomicLong();
      reports.iterator().forEachRemaining(r -> count.addAndGet(r.getReportCount()));

      aggregate.setReportCount(count.get());
      ctx.output(aggregate.build());
    }
  }

}