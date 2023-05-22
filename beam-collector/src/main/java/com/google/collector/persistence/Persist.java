package com.google.collector.persistence;

import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;

/**
 * Factory class used to load pre-defined transforms that can write constellation objects to various
 * sinks.
 */
public final class Persist {
  private Persist() {}
  public static PersistenceTransform toBigTable(
      CloudBigtableTableConfiguration bigtableTableConfig) {
    return new ToBigTable(bigtableTableConfig);
  }

  public static PersistenceTransform toJson(String outputFilePath) {
    return new ToJson(outputFilePath);
  }

}
