package com.damonio.template;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.BigQueryEmulatorContainer;

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
    public BigQueryEmulatorContainer bigQueryEmulatorContainer() {
        var container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3");
        container.setCommandParts(new String[]{"--project=" + PROJECT_NAME});
        container.start();
        return container;
    }

    @Bean
    @ConditionalOnMissingBean
    public BigQueryTemplate migrationHistoryTableCreator(BigQuery bigQuery) {
        return new BigQueryTemplate(bigQuery);
    }

}
