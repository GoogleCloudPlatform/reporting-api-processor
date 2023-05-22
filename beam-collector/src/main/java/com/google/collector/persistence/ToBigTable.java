package com.google.collector.persistence;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import constellations.ConstellationOuterClass.Constellation;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hbase.thirdparty.com.google.gson.Gson;

/**
 * Implementation of BigTable sink. This class allows the pipeline to write output constellations to
 * BigTable.
 */
final class ToBigTable extends PersistenceTransform {

  private final CloudBigtableTableConfiguration bigtableTableConfig;

  public ToBigTable(CloudBigtableTableConfiguration bigtableTableConfig) {
    this.bigtableTableConfig = bigtableTableConfig;
  }

  @Override
  public PDone expand(PCollection<Constellation> input) {
    return input.apply(ParDo.of(new DoFn<Constellation, Mutation>() {
      @ProcessElement
      public void processElement(ProcessContext ctx) {
        Constellation element = ctx.element();

        long timestamp = System.currentTimeMillis();
        Put row = new Put(Bytes.toBytes(element.getId()));
        String json = new Gson().toJson(element);

        row.addColumn(
            Bytes.toBytes("processed_reports"),
            Bytes.toBytes("report"),
            timestamp,
            Bytes.toBytes(json)
        );

        ctx.output(row);
      }
    }))
        .apply(CloudBigtableIO.writeToTable(bigtableTableConfig));
  }
}
