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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import constellations.ConstellationOuterClass.Constellation;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

/**
 * Implementation of JSON sink. This class allows the pipeline to write output constellations to
 * JSON files in the local file system.
 */
class ToJson extends PersistenceTransform {

  private final String outputFilePath;

  ToJson(String outputFilePath) {
    this.outputFilePath = outputFilePath;
  }

  @Override
  public PDone expand(PCollection<Constellation> input) {
    return input
        .apply(ParDo.of(new DoFn<Constellation, String>() {
          @ProcessElement
          public void processElement(ProcessContext ctx) throws InvalidProtocolBufferException {
            Constellation element = ctx.element();
            String print = JsonFormat.printer().print(element);
            ctx.output(print);
          }
        }))
        .apply(TextIO.write().to(outputFilePath));
  }
}