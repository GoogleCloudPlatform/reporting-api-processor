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
