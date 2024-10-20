package com.damonio.migration;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "big-query-migration-tool")
public class BigQueryMigrationServiceConfiguration {
    @Builder.Default
    private String onlyOnceRunPrefix = "O";
    @Builder.Default
    private String scriptLocation = "big-query" + File.separator + "migrations";
    private String projectId;
    private String datasetId;

    @PostConstruct
    public void init() {
        log.info("BigQueryMigrationServiceConfigurations: [{}]", this);
    }
}


