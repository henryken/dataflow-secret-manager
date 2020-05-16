package com.henrysuryawirawan.dataflowsecretmanager;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

public interface MainPipelineOptions extends DataflowPipelineOptions {

  @Description("JDBC URL Secret Name")
  @Default.String("jdbc-url-secret")
  @Validation.Required
  ValueProvider<String> getJdbcUrlSecretName();
  void setJdbcUrlSecretName(ValueProvider<String> jdbcUrlSecretName);

}
