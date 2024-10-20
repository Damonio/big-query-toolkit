package com.damonio.sample;

import com.damonio.migration.MigrationLog;
import com.damonio.template.BigQueryTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanMigrateDatabaseAsAJarTest extends BaseTest {

    @Autowired
    private BigQueryTemplate bigQueryTemplate;


    @TestConfiguration
    static class ThisTestTime {

        @Bean
        public Clock clock() {
            return Clock.fixed(Instant.parse("2024-10-20T17:47:26.123456Z"), ZoneId.systemDefault());
        }
    }

    @Test
    @SneakyThrows
    void contextLoads() {
        var migrations = bigQueryTemplate.execute("SELECT * FROM `test_dataset.migration_log`", MigrationLog.class);

        assertEquals(getExpected(), new ObjectMapper().findAndRegisterModules().writeValueAsString(migrations));
    }

    private static @NotNull String getExpected() {
        return """
                [
                {"installRank":1,"fileName":"O1_create_table_1.sql","successful":true,"checksum":"bd05e39631815d15f3b5de2a5c8bde5264d303f1a2211355f6ea8e5344efe38a","executionLocalDateTime":"2024-10-20T13:47:26.123456"},
                {"installRank":2,"fileName":"O2_create_table_2.sql","successful":true,"checksum":"f22a315aacddf3d5b99eb684766027710354b4c49aa54eb11df07b60a940488a","executionLocalDateTime":"2024-10-20T13:47:26.123456"},
                {"installRank":3,"fileName":"1_populate_table_1.sql","successful":true,"checksum":"6b0e83502affda2185864e985ae1343082defd8ecc9e0e37f9e73414a79fa74a","executionLocalDateTime":"2024-10-20T13:47:26.123456"},
                {"installRank":4,"fileName":"2-populate_table_2.sql","successful":true,"checksum":"7938e17c3cde7f2527bf7476ece5203a59d7d9af1dbb33a387a800b4baf2ee40","executionLocalDateTime":"2024-10-20T13:47:26.123456"}
                ]
                """.replace("\n", "").trim();
    }


}
