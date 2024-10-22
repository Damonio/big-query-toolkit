package com.damonio.migration.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GenericBigQueryMigratorWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenericBigQueryMigratorWebApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

}