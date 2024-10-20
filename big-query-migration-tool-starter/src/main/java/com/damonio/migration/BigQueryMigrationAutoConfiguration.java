package com.damonio.migration;

import com.damonio.template.BigQueryTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration
class BigQueryMigrationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BigQueryMigrationService bigQueryMigrationService(Clock clock, BigQueryTemplate template,
                                                             BigQueryVersionService bigQueryVersionService,
                                                             BigQueryMigrationServiceConfiguration bigQueryMigrationServiceConfiguration) {
        return new BigQueryMigrationService(clock, template, bigQueryVersionService, bigQueryMigrationServiceConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    public BigQueryVersionService bigQueryVersionService(BigQueryTemplate template) {
        return new BigQueryVersionService(template);
    }

    @Bean
    @ConditionalOnMissingBean
    public BigQueryMigrationServiceConfiguration bigQueryMigrationServiceConfiguration() {
        return new BigQueryMigrationServiceConfiguration();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }


}
