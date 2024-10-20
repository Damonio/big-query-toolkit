package com.damonio.migration;


import com.damonio.template.BigQueryTemplate;
import com.google.cloud.bigquery.BigQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.damonio.migration.Util.generateChecksum;
import static com.damonio.migration.Util.readFile;
import static com.damonio.migration.Util.substituteValues;

@Slf4j
class BigQueryMigrationService {

    private final Clock clock;
    private final BigQueryTemplate bigQueryTemplate;
    private final BigQueryVersionService bigQueryVersionService;
    private final BigQueryMigrationServiceConfiguration bigQueryMigrationServiceConfiguration;

    BigQueryMigrationService(Clock clock, BigQueryTemplate bigQueryTemplate, BigQueryVersionService bigQueryVersionService, BigQueryMigrationServiceConfiguration bigQueryMigrationServiceConfiguration) {
        this.clock = clock;
        this.bigQueryTemplate = bigQueryTemplate;
        this.bigQueryVersionService = bigQueryVersionService;
        this.bigQueryMigrationServiceConfiguration = bigQueryMigrationServiceConfiguration;
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

    private void migrateClientScripts() {
        var migrationScripts = getMigrationScripts();
        log.info("Scripts found: [{}]", migrationScripts);
        var appliedMigrations = bigQueryTemplate.execute("SELECT * FROM `test_dataset.migration_log` WHERE successful = FALSE", MigrationLog.class);
        var singleRunScriptsAlreadyExecuted = appliedMigrations.parallelStream().map(MigrationLog::getFileName).filter(this::isOnlyOnceRunScript).toList();
        migrationScripts.removeAll(singleRunScriptsAlreadyExecuted);
        var latestOnlyOnceScriptMigrationNumber = singleRunScriptsAlreadyExecuted.parallelStream().filter(this::isOnlyOnceRunScript).map(this::getOnlyOnceScriptNumber).mapToInt(Integer::intValue).max().orElse(0);
        var nonOnlyOnceRunScripts = migrationScripts.parallelStream().filter(migration -> !isOnlyOnceRunScript(migration)).toList();
        var filteredNotRunOnlyOnceRunMigrations = migrationScripts.parallelStream().filter(this::isOnlyOnceRunScript).filter(migration -> getOnlyOnceScriptNumber(migration) > latestOnlyOnceScriptMigrationNumber).toList();
        validateNotDuplicatedRuns(filteredNotRunOnlyOnceRunMigrations);
        var scriptsToExecute = new ArrayList<>(filteredNotRunOnlyOnceRunMigrations);
        scriptsToExecute.addAll(nonOnlyOnceRunScripts);
        log.info("Next scripts will be executed in the following order: [{}]", scriptsToExecute);
        scriptsToExecute.forEach(this::tryExecuteMigration);
    }

    private void validateNotDuplicatedRuns(List<String> filteredNotRunOnlyOnceRunMigrations) {
        var versions = filteredNotRunOnlyOnceRunMigrations.parallelStream().map(this::getOnlyOnceScriptNumber).toList();
        var uniqueVersions = new HashSet<>(versions);
        if (filteredNotRunOnlyOnceRunMigrations.size() == uniqueVersions.size()) {
            return;
        }
        log.error("Duplicated only once run scripts to execute found in the migration list");
        throw new DuplicateOnlyOnceRunScriptsVersionsFound();
    }

    private static class DuplicateOnlyOnceRunScriptsVersionsFound extends RuntimeException {}

    private boolean isOnlyOnceRunScript(String string) {
        return string.startsWith(bigQueryMigrationServiceConfiguration.getOnlyOnceRunPrefix()) && string.contains("_");
    }

    private Integer getOnlyOnceScriptNumber(String migration) {
        return Integer.parseInt(migration.split(bigQueryMigrationServiceConfiguration.getOnlyOnceRunPrefix())[1].split("_")[0]);
    }

    @SneakyThrows
    private List<String> getMigrationScripts() {
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath*:"+bigQueryMigrationServiceConfiguration.getScriptLocation()+ File.separator +"*");
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
        log.info("Migration script [{}]", fileName);
        var migrationScript = readFile(bigQueryMigrationServiceConfiguration.getScriptLocation() + File.separator + fileName);
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
        valuesMap.put("execution_local_date_time", toBigQueryLocalDateTime());

        var resolvedString = substituteValues(insertQuery(), valuesMap);
        log.info("Initializing database using script [{}]", resolvedString);
        bigQueryTemplate.execute(resolvedString);
    }

    private String toBigQueryLocalDateTime() {
        return LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSS"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MigrationCountHolder {
        private Integer count;
    }

    private static String insertQuery() {
        return """
                INSERT INTO `test_dataset.migration_log` (install_rank, file_name, successful, checksum, execution_local_date_time)
                   VALUES (${nextInstallRank}, '${fileName}', ${status}, '${checksum}', '${execution_local_date_time}');
                """;
    }

}


