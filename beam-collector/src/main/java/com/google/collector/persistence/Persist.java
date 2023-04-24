package com.google.collector.persistence;

import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;

public final class Persist {
  public static PersistenceTransform toBigTable(
      CloudBigtableTableConfiguration bigtableTableConfig) {
    return new ToBigTable(bigtableTableConfig);
  }

  public static PersistenceTransform toJson(String outputFilePath) {
    return new ToJson(outputFilePath);
  }

}
