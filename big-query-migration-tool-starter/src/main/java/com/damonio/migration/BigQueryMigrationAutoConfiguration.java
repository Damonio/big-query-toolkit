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
                                                             BigQueryMigrationConfiguration bigQueryMigrationConfiguration) {
        return new BigQueryMigrationService(clock, template, bigQueryMigrationConfiguration);
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
