package com.damonio.migration;


import com.damonio.template.BigQueryTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.damonio.migration.Util.generateChecksum;
import static com.damonio.migration.Util.readFile;
import static com.damonio.migration.Util.substituteValues;

@Slf4j
class BigQueryMigrationService {

    private final BigQueryTemplate bigQueryTemplate;
    private final BigQueryVersionService bigQueryVersionService;

    BigQueryMigrationService(BigQueryTemplate bigQueryTemplate, BigQueryVersionService bigQueryVersionService) {
        this.bigQueryTemplate = bigQueryTemplate;
        this.bigQueryVersionService = bigQueryVersionService;
        migrate();
    }

    public void migrate() {
        bigQueryVersionService.createMigrationHistoryTableIfDoesntExists();
        validateNoFailedMigrations();
        migrateClientScripts();
    }

    private void validateNoFailedMigrations() {
        var failedMigrations = bigQueryTemplate.execute("SELECT * FROM `test_dataset.migration_log` WHERE successful = FALSE", MigrationLog.class);
        if (!CollectionUtils.isEmpty(failedMigrations)) throw new FailedMigrationsFound();
    }

    private static class FailedMigrationsFound extends RuntimeException {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MigrationLog {
        private Integer installRank;
        private String fileName;
        private Boolean successful;
    }

    private void migrateClientScripts() {
        var migrationScripts = getMigrationScripts();
        log.info("Scripts found: [{}]", migrationScripts);
        var appliedMigrations = bigQueryTemplate.execute("SELECT * FROM `test_dataset.migration_log` WHERE successful = FALSE", MigrationLog.class);
        var singleRunScriptsAlreadyExecuted = appliedMigrations.parallelStream().map(MigrationLog::getFileName).filter(BigQueryMigrationService::isSingleRunScript).toList();
        migrationScripts.removeAll(singleRunScriptsAlreadyExecuted);
        var latestSingleRunScriptMigrationNumber = singleRunScriptsAlreadyExecuted.parallelStream().filter(BigQueryMigrationService::isSingleRunScript).map(BigQueryMigrationService::getSingleRunScriptNumber).mapToInt(Integer::intValue).max().orElse(0);
        var nonSingleRunScripts = migrationScripts.parallelStream().filter(migration -> !isSingleRunScript(migration)).toList();
        var filteredOlderSingleRunMigrations = migrationScripts.parallelStream().filter(BigQueryMigrationService::isSingleRunScript).filter(migration -> getSingleRunScriptNumber(migration) > latestSingleRunScriptMigrationNumber).toList();
        var scriptsToExecute = new ArrayList<>(nonSingleRunScripts);
        scriptsToExecute.addAll(filteredOlderSingleRunMigrations);
        Util.order('V', scriptsToExecute);
        log.info("Scripts to execute: [{}]", scriptsToExecute);
        scriptsToExecute.forEach(this::tryExecuteMigration);
    }

    private static boolean isSingleRunScript(String string) {
        return string.startsWith("V") && string.contains("_");
    }

    private static Integer getSingleRunScriptNumber(String migration) {
        return Integer.parseInt(migration.split("V")[1].split("_")[0]);
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
        insertSuccessfulMigration(fileName, generateChecksum(migrationScript));
    }

    private void insertFailedMigration(String fileName) {
        insertMigration(fileName, "FALSE", "");
    }

    private void insertSuccessfulMigration(String fileName, String checksum) {
        insertMigration(fileName, "TRUE", checksum);
    }

    private void insertMigration(String fileName, String status, String checksum) {
        var nextMigration = bigQueryTemplate.execute("SELECT COUNT(1) AS count FROM `test_dataset.migration_log`", MigrationCountHolder.class).get(0).getCount() + 1;

        var valuesMap = new HashMap<String, String>();
        valuesMap.put("nextInstallRank", Integer.toString(nextMigration));
        valuesMap.put("fileName", fileName);
        valuesMap.put("status", status);
        valuesMap.put("checksum", checksum);

        var resolvedString = substituteValues(insertQuery(), valuesMap);
        log.info("Initializing database using script [{}]", resolvedString);
        bigQueryTemplate.execute(resolvedString);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MigrationCountHolder {
        private Integer count;
    }

    private static String insertQuery() {
        return """
                INSERT INTO `test_dataset.migration_log` (install_rank, file_name, successful, checksum)
                   VALUES (${nextInstallRank}, '${fileName}', ${status}, '${checksum}');
                """;
    }

}


