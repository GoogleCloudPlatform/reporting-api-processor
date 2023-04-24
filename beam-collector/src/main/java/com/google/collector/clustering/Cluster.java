package com.google.collector.clustering;

import constellations.ConstellationOuterClass.Constellation;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import reports.SecurityReportOuterClass.SecurityReport;

public class Cluster extends
    PTransform<PCollection<SecurityReport>, PCollection<Constellation>> {

  @Override
  public PCollection<Constellation> expand(PCollection<SecurityReport> input) {
    return input
        .apply("ExtractClusterKey", ParDo.of(new ExtractClusterKey()))
        .apply("GroupByKey", GroupByKey.create())
        .apply("CreateConstellations", ParDo.of(new CreateConstellation()));
  }
}
