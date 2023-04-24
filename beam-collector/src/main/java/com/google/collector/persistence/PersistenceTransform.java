package com.google.collector.persistence;

import constellations.ConstellationOuterClass.Constellation;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import reports.SecurityReportOuterClass.SecurityReport;

public abstract class PersistenceTransform extends
    PTransform<PCollection<Constellation>, PDone> {
}
