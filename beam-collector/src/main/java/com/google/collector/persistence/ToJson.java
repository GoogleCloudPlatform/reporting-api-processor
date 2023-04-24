package com.google.collector.persistence;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import constellations.ConstellationOuterClass.Constellation;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

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