package com.damonio.migration;


import com.damonio.template.BigQueryTemplate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.damonio.migration.Util.readFile;

@Slf4j
@RequiredArgsConstructor
public class BigQueryMigrationService {

    private final BigQueryTemplate bigQueryTemplate;
    private final BigQueryVersionService bigQueryVersionService;

    @PostConstruct
    public void migrate() {
        bigQueryVersionService.createMigrationHistoryTableIfDoesntExists();
        migrateClientScripts();
    }

    private void migrateClientScripts() {
        var migrationScripts = getMigrationScripts();
        log.info("Scripts found: [{}]", migrationScripts);
        migrationScripts.forEach(this::tryExecuteMigration);
    }

    @SneakyThrows
    private static List<String> getMigrationScripts() {
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath*:big-query/migrations/*");
        return Arrays.stream(resources).map(Resource::getFilename).collect(Collectors.toList());
    }

    private void tryExecuteMigration(String fileName) {
        try {
            executeMigration(fileName);
        } catch (Exception e) {
            log.error("Failed to execute migration script [{}]", fileName, e);
            insertFailedMigration(fileName);
        }
    }

    private void executeMigration(String fileName) {
        var migrationScript = readFile("big-query/migrations/" + fileName);
        bigQueryTemplate.execute(migrationScript);
        insertSuccessfulMigration(fileName);
    }

    private void insertFailedMigration(String fileName) {
        insertMigration(fileName, "failed");
    }

    private void insertSuccessfulMigration(String fileName) {
        insertMigration(fileName, "success");
    }

    private void insertMigration(String fileName, String status) {
        var migrationScript = "INSERT INTO `test_dataset.migration_history` (install_rank, file_name, successful, checksum) VALUES (0, '" + fileName + "', '" + status + "', '');";
        bigQueryTemplate.execute(migrationScript);
    }

}
