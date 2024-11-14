package com.damonio.sample;

import com.damonio.template.BigQueryTemplate;
import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.BigQueryEmulatorContainer;

@Configuration
public class BigQueryConfiguration {

    @Bean
    public BigQuery bigQuery() {
        return bigQuery;
    }

    private BigQuery bigQuery = initialize("test_project");

    private BigQuery initialize(String projectName) {
        var container = getBigQueryEmulatorContainer(projectName);
        return client(projectName, container);
    }

    private static BigQuery client(String projectId, BigQueryEmulatorContainer container) {
        var url = container.getEmulatorHttpEndpoint();
        var options = BigQueryOptions
                .newBuilder()
                .setProjectId(projectId)
                .setHost(url)
                .setLocation(url)
                .setCredentials(NoCredentials.getInstance())
                .build();
        return options.getService();
    }

    private static @NotNull BigQueryEmulatorContainer getBigQueryEmulatorContainer(String testProject) {
        var container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3");
        container.setCommandParts(new String[]{"--project=" + testProject});
        container.start();
        return container;
    }

    @Bean
    public BigQueryTemplate getBigQuery() {
        return new BigQueryTemplate(bigQuery);
    }
}
