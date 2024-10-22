package com.damonio.migration;

import com.damonio.template.BigQueryTemplate;
import com.google.api.services.bigquery.Bigquery;
import com.google.cloud.bigquery.BigQuery;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration
class BigQueryMigrationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BigQueryMigrationService bigQueryMigrationService(Clock clock, BigQuery bigQuery, BigQueryTemplate template,
                                                             BigQueryMigrationConfiguration bigQueryMigrationConfiguration) {
        return new BigQueryMigrationService(clock, bigQuery, "classpath*:", template, bigQueryMigrationConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    public BigQueryMigrationConfiguration bigQueryMigrationServiceConfiguration() {
        return new BigQueryMigrationConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }


}
