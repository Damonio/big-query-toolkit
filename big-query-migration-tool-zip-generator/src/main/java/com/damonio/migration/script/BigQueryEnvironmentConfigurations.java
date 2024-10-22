package com.damonio.migration.script;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "big-query-environment-configurations")
public class BigQueryEnvironmentConfigurations {
    private String projectId;
    private String datasetId;
    private Map<String, String> replacements = new HashMap<>();
}
