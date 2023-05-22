package com.google.collector.persistence;

import constellations.ConstellationOuterClass.Constellation;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

/**
 * Marker interface used to persist constellations to various data sinks. Users wanting to extend this
 * pipeline to write to new sinks should implement transforms that inherit from this class.
 */
public abstract class PersistenceTransform extends
    PTransform<PCollection<Constellation>, PDone> {
}
