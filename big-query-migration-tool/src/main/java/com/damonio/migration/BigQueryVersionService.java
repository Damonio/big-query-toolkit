package com.damonio.migration;


import com.damonio.template.BigQueryTemplate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.damonio.migration.Util.*;

@Slf4j
@RequiredArgsConstructor
class BigQueryVersionService {

    private final BigQueryTemplate bigQueryTemplate;
    private final BigQueryMigrationConfiguration bigQueryMigrationConfiguration;

    void createMigrationHistoryTableIfDoesntExists() {
        var migrationHistoryTableScript = readFileInsideJar("scripts/migration_history_table.sql");

        var valuesMap = new HashMap<String, String>();
        valuesMap.put("default_dataset", "test_dataset");

        var resolvedString = substituteValues(migrationHistoryTableScript, valuesMap);
        log.info("Initializing database using script [{}]", resolvedString);
        bigQueryTemplate.execute(resolvedString);
    }
}
