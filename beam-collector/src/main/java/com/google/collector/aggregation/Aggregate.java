package com.google.collector.aggregation;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import reports.SecurityReportOuterClass.SecurityReport;

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