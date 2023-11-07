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

package com.google.collector.clustering;

import constellations.ConstellationOuterClass.Constellation;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import reports.SecurityReportOuterClass.SecurityReport;

/**
 * Simple cluster transform. This class encapsulates the steps needed to transform a group of
 * individual reports into clusters of reports, aka constellations.
 */
public final class Cluster extends
    PTransform<PCollection<SecurityReport>, PCollection<Constellation>> {

  @Override
  public PCollection<Constellation> expand(PCollection<SecurityReport> input) {
    return input
        // build cluster keys depending on which reporting API features these reports belong to
        .apply("ExtractClusterKey", ParDo.of(new ExtractClusterKey()))
        // cluster reports by cluster key
        .apply("GroupByKey", GroupByKey.create())
        // compute aggregate metrics and reduce clusters into constellation objects
        .apply("CreateConstellations", ParDo.of(new CreateConstellation()));
  }
}
