package com.damonio.template;

import com.google.cloud.bigquery.BigQuery;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
class BigQueryTemplateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BigQueryTemplate migrationHistoryTableCreator(BigQuery bigQuery) {
        return new BigQueryTemplate(bigQuery);
    }
}
