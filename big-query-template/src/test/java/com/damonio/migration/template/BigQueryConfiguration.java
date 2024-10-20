package com.damonio.migration.template;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.ClientContext;
import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.BigQueryEmulatorContainer;

import java.io.IOException;

@Configuration
public class BigQueryConfiguration {

    private final static String PROJECT_NAME = "test_project";
    private final static String DATASET_NAME = "test_dataset";

    @Bean
    public BigQuery bigQuery(BigQueryEmulatorContainer container) {
        var emulatorHttpEndpoint = container.getEmulatorHttpEndpoint();
        var options = BigQueryOptions.newBuilder().setProjectId(PROJECT_NAME)
                .setHost(emulatorHttpEndpoint).setLocation(emulatorHttpEndpoint).setCredentials(NoCredentials.getInstance()).build();
        var bigQuery = options.getService();
        var datasetInfo = DatasetInfo.newBuilder(DatasetId.of(PROJECT_NAME, DATASET_NAME)).build();
        bigQuery.create(datasetInfo);
        return bigQuery;
    }

    @Bean
    @SneakyThrows
    public BigQueryWriteClient bigQueryWriteClient(BigQueryEmulatorContainer container) {
        //TODO use the container GRCP endpoint if they expose it in the future
        var build = BigQueryWriteSettings.newBuilder().setEndpoint("0.0.0.0:9060").setCredentialsProvider(new NoCredentialsProvider()).build();
        return BigQueryWriteClient.create(build);
    }

    @SneakyThrows
    private static BigQueryWriteSettings getBuild(ClientContext context)  {
        return BigQueryWriteSettings.newBuilder().build();
    }

    @Bean
    public BigQueryEmulatorContainer bigQueryEmulatorContainer() {
        var container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3");
        container.setCommandParts(new String[]{"--project=" + PROJECT_NAME});
        container.start();
        return container;
    }

}
