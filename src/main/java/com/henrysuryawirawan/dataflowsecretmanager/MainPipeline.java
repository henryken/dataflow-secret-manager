package com.henrysuryawirawan.dataflowsecretmanager;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import java.io.IOException;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.BigEndianIntegerCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.RowMapper;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainPipeline {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainPipeline.class);

  public static void main(String[] args) {
    PipelineOptionsFactory.register(MainPipelineOptions.class);

    MainPipelineOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(MainPipelineOptions.class);

    NestedValueProvider<String, String> jdbcUrlValueProvider = NestedValueProvider
        .of(options.getJdbcUrlSecretName(), secretName -> {
          try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            AccessSecretVersionResponse response = client.accessSecretVersion(secretName);

            String jdbcUrl = response.getPayload().getData().toStringUtf8();

            return jdbcUrl;
          } catch (IOException e) {
            throw new RuntimeException("Unable to read JDBC URL secret");
          }
        });

    Pipeline pipeline = Pipeline.create(options);

    pipeline
        .apply("SQLServer Read - JDBC",
            JdbcIO.<KV<Integer, String>>read()
                .withDataSourceConfiguration(JdbcIO.DataSourceConfiguration.create(
                    StaticValueProvider.of("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
                    jdbcUrlValueProvider)
                )
                .withQuery("select * from Sales.Customers_Archive")
                .withCoder(KvCoder.of(BigEndianIntegerCoder.of(), StringUtf8Coder.of()))
                .withRowMapper((RowMapper<KV<Integer, String>>) resultSet ->
                    KV.of(resultSet.getInt(1), resultSet.getString(2))))

        .apply(ParDo.of(new DoFn<KV<Integer, String>, String>() {

          @ProcessElement
          public void processElement(@Element KV<Integer, String> element,
              OutputReceiver<String> out) {
            Integer customerId = element.getKey();
            String customerName = element.getValue();

            LOGGER.info("Customer: (" + customerId + ", " + customerName + ")");
          }

        }));

    pipeline.run();
  }

}
