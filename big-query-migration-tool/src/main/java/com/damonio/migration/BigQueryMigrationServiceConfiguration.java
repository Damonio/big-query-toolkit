package com.damonio.migration;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "big-query-migration-tool")
class BigQueryMigrationServiceConfiguration {
    private String onlyOnceRunPrefix = "O";
    private String scriptLocation = "big-query" + File.separator + "migrations";
}


