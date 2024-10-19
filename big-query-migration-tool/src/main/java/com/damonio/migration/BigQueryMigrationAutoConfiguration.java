package com.damonio.migration;

import com.damonio.template.BigQueryTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
class BigQueryMigrationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BigQueryMigrationService bigQueryMigrationService(BigQueryTemplate template, BigQueryVersionService bigQueryVersionService) {
        return new BigQueryMigrationService(template, bigQueryVersionService);
    }


}
