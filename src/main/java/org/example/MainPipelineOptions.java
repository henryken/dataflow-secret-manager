package org.example;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

public interface MainPipelineOptions extends DataflowPipelineOptions {

  @Description("SqlServer Jdbc URL Secret Name")
  @Default.String("jdbc-url-secret")
  @Validation.Required
  ValueProvider<String> getSqlServerJdbcUrlSecretName();
  void setSqlServerJdbcUrlSecretName(ValueProvider<String> sqlServerJdbcUrlSecretName);

}
