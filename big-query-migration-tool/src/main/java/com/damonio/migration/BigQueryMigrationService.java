package com.damonio.migration;


import com.damonio.template.BigQueryTemplate;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
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

import static com.damonio.migration.MigrationUtil.generateChecksum;
import static com.damonio.migration.MigrationUtil.readFile;
import static com.damonio.migration.MigrationUtil.readFileInsideJar;
import static com.damonio.migration.MigrationUtil.substituteValues;

@Slf4j
public class BigQueryMigrationService {

    private final Clock clock;
    private final String scriptLocation;
    private final BigQueryTemplate bigQueryTemplate;
    private final BigQueryMigrationConfiguration bigQueryMigrationConfiguration;

    public BigQueryMigrationService(Clock clock, String scriptLocation, BigQueryTemplate bigQueryTemplate, BigQueryMigrationConfiguration bigQueryMigrationConfiguration) {
        this.clock = clock;
        this.scriptLocation = scriptLocation;
        this.bigQueryTemplate = bigQueryTemplate;
        this.bigQueryMigrationConfiguration = bigQueryMigrationConfiguration;
        migrate();
    }

    public void migrate() {
        initializeDataset();
        validateNoFailedMigrations();
        migrateClientScripts();
    }

    void initializeDataset() {
        createDatasetIfDoesntExists();
        createMigrationHistoryTableIfDoesntExists();
    }

    private void createDatasetIfDoesntExists() {
        var dataset = bigQueryTemplate.getBigQuery().getDataset(DatasetId.of(bigQueryMigrationConfiguration.getProjectId(), bigQueryMigrationConfiguration.getDatasetId()));
        if (dataset != null) {
            return;
        }
        log.info("Project [{}] does not contain the dataset [{}]", bigQueryMigrationConfiguration.getProjectId(), bigQueryMigrationConfiguration.getDatasetId());
        var build = DatasetInfo.newBuilder(DatasetId.of(bigQueryMigrationConfiguration.getProjectId(), bigQueryMigrationConfiguration.getDatasetId())).build();
        bigQueryTemplate.getBigQuery().create(build);
        log.info("Project [{}] has now dataset [{}]", bigQueryMigrationConfiguration.getProjectId(), bigQueryMigrationConfiguration.getDatasetId());
    }

    private void createMigrationHistoryTableIfDoesntExists() {
        var migrationHistoryTableScript = readFileInsideJar("scripts/migration_log_table.sql");

        var valuesMap = new HashMap<String, String>();
        valuesMap.put("default_dataset", "test_dataset");

        var resolvedString = substituteValues(migrationHistoryTableScript, valuesMap);
        log.info("Initializing database using script [{}]", resolvedString);
        bigQueryTemplate.execute(resolvedString);
    }

    private void validateNoFailedMigrations() {
        var failedMigrations = bigQueryTemplate.execute("SELECT * FROM `test_dataset.migration_log` WHERE successful = FALSE", MigrationLog.class);
        if (!CollectionUtils.isEmpty(failedMigrations)) throw new FailedMigrationsFound();
    }

    private static class FailedMigrationsFound extends RuntimeException {
    }

    private void migrateClientScripts() {
        var allMigrationScriptsFound = getMigrationScripts();
        log.info("Scripts found: [{}]", allMigrationScriptsFound);
        var appliedMigrations = bigQueryTemplate.execute("SELECT * FROM `test_dataset.migration_log` WHERE successful = FALSE", MigrationLog.class);
        var singleRunScriptsAlreadyExecuted = appliedMigrations.parallelStream().map(MigrationLog::getFileName).filter(this::isOnlyOnceRunScript).toList();
        allMigrationScriptsFound.removeAll(singleRunScriptsAlreadyExecuted);
        var latestOnlyOnceScriptMigrationNumber = singleRunScriptsAlreadyExecuted.parallelStream().filter(this::isOnlyOnceRunScript).map(this::getOnlyOnceScriptNumber).mapToInt(Integer::intValue).max().orElse(0);
        var nonOnlyOnceRunScripts = allMigrationScriptsFound.parallelStream().filter(migration -> !isOnlyOnceRunScript(migration)).toList();
        var nonOnlyOnceRunScriptsWithDifferentChecksum = nonOnlyOnceRunScripts.parallelStream().filter(migration -> needsToBeReRun(migration, appliedMigrations)).toList();
        var filteredNotRunOnlyOnceRunMigrations = allMigrationScriptsFound.parallelStream().filter(this::isOnlyOnceRunScript).filter(migration -> getOnlyOnceScriptNumber(migration) > latestOnlyOnceScriptMigrationNumber).toList();
        validateNotDuplicatedRuns(filteredNotRunOnlyOnceRunMigrations);
        var scriptsToExecute = new ArrayList<>(filteredNotRunOnlyOnceRunMigrations);
        scriptsToExecute.addAll(nonOnlyOnceRunScriptsWithDifferentChecksum);
        log.info("Next scripts will be executed in the following order: [{}]", scriptsToExecute);
        scriptsToExecute.forEach(this::tryExecuteMigration);
    }

    private boolean needsToBeReRun(String migration, List<MigrationLog> appliedMigrations) {
        var appliedMigrationsByName = appliedMigrations.parallelStream().collect(Collectors.groupingBy(MigrationLog::getFileName));
        if (!appliedMigrationsByName.containsKey(migration)) {
            return true;
        }
        var migrationLogs = appliedMigrationsByName.get(migration).get(0);
        return !migrationLogs.getChecksum().matches(generateChecksum(getScript(migration)));
    }

    private String getScript(String migration) {
        return readFile(bigQueryMigrationConfiguration.getScriptLocation() + File.separator + migration);
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

    private static class DuplicateOnlyOnceRunScriptsVersionsFound extends RuntimeException {
    }

    private boolean isOnlyOnceRunScript(String string) {
        return string.startsWith(bigQueryMigrationConfiguration.getOnlyOnceRunPrefix()) && string.contains("_");
    }

    private Integer getOnlyOnceScriptNumber(String migration) {
        return Integer.parseInt(migration.split(bigQueryMigrationConfiguration.getOnlyOnceRunPrefix())[1].split("_")[0]);
    }

    @SneakyThrows
    private List<String> getMigrationScripts() {
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources(scriptLocation + bigQueryMigrationConfiguration.getScriptLocation() + File.separator + "*");
        return Arrays.stream(resources).map(Resource::getFilename).collect(Collectors.toCollection(ArrayList::new));
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
        var migrationScript = getScript(fileName);
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
        var nextMigration = bigQueryTemplate.execute("SELECT COALESCE(MAX(install_rank), 0) AS max_install_rank FROM `test_dataset.migration_log`", MigrationCountHolder.class).get(0).getMaxInstallRank() + 1;

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
        return getNow().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"));
    }

    private LocalDateTime getNow() {
        return LocalDateTime.now(clock);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MigrationCountHolder {
        private Integer maxInstallRank;
    }

    private static String insertQuery() {
        return """
                INSERT INTO `test_dataset.migration_log` (install_rank, file_name, successful, checksum, execution_local_date_time)
                   VALUES (${nextInstallRank}, '${fileName}', ${status}, '${checksum}', '${execution_local_date_time}');
                """;
    }

}


