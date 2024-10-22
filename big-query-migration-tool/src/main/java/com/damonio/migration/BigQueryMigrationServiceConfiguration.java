package com.damonio.migration;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@With
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ConfigurationProperties(prefix = "big-query-migration-configuration")
public class BigQueryMigrationServiceConfiguration {
    @Builder.Default
    private String onlyOnceRunPrefix = "O";
    @Builder.Default
    private String scriptLocation = "big-query" + File.separator + "migrations";
    private String projectId;
    private String datasetId;
    @Builder.Default
    private Map<String, String> replacements = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("BigQueryMigrationServiceConfigurations: [{}]", this);
    }
}


