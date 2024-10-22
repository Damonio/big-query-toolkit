package com.damonio.migration.web.migration;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static com.damonio.migration.web.migration.GenericMigrationUtil.readConfigurationFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GenericMigrationUtilTest {

    @Test
    @SneakyThrows
    void canReaConfigurationFile() {
        var configurationFile = new ClassPathResource("application-test.yml").getFile();

        var configuration = readConfigurationFile("", configurationFile.getAbsolutePath(), "big-query-migration-configuration");

        assertEquals(getExpected(), new ObjectMapper().findAndRegisterModules().writeValueAsString(configuration));
    }

    private static @NotNull String getExpected() {
        return """
                {
                "onlyOnceRunPrefix":"O",
                "scriptLocation":"big-query/migrations",
                "projectId":"bigquery-public-data",
                "datasetId":"bigquery-public-data",
                "replacements":{"test-replacement":"test-value"}
                }
                """.trim().replace("\n", "");
    }
}