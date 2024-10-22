package com.damonio.migration.web.migration;

//import com.damonio.migration.BigQueryMigrationService;
//import com.damonio.migration.BigQueryMigrationServiceConfiguration;
//import com.damonio.template.BigQueryTemplate;

import com.damonio.migration.BigQueryMigrationService;
import com.damonio.migration.BigQueryMigrationServiceConfiguration;
import com.damonio.template.BigQueryTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.cloud.bigquery.BigQuery;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.lingala.zip4j.ZipFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class GenericMigrationService {

    private final Clock clock;

    public void migrate(String environmentFileName, String credentials, MultipartFile migrationScripts) {
        var extractedLocation = tryExtractFile(migrationScripts);
        var bigQueryMigrationService = tryReadConfigurationFile(environmentFileName, extractedLocation);
        new BigQueryMigrationService(clock, new BigQueryTemplate(getBigQuery(credentials)), bigQueryMigrationService).migrate();
    }

    private static BigQuery getBigQuery(String credentials) {
//        BigQuery bigQuery =
        return null;
    }

    private String tryExtractFile(MultipartFile migrationScripts) {
        try {
            return extractFile(migrationScripts);
        } catch (Exception e) {
            throw new FailedToExtractZip(e);
        }
    }

    private static BigQueryMigrationServiceConfiguration tryReadConfigurationFile(String environmentFileName, String extractedLocation) {
        try {
            return readConfigurationFile(environmentFileName, extractedLocation);
        } catch (Exception e) {
            throw new FailedToReadEnvironmentFile(e);
        }
    }


    @SneakyThrows
    private static BigQueryMigrationServiceConfiguration readConfigurationFile(String environmentFileName, String extractedLocation) {
        var mapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
        var bigQueryMigrationServiceConfiguration = mapper.readValue(new File(extractedLocation + File.separator + environmentFileName), BigQueryMigrationServiceConfiguration.class);
        return bigQueryMigrationServiceConfiguration.withScriptLocation(extractedLocation + File.separator + bigQueryMigrationServiceConfiguration.getScriptLocation());
    }

    private String extractFile(MultipartFile migrationScripts) {
        var zip = saveToTempFolder(migrationScripts);
        return unzipFile(zip);
    }

    @SneakyThrows
    private String unzipFile(File zip) {
        var destination = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID();
        var zipFile = new ZipFile(zip);
        zipFile.extractAll(destination);
        return destination;
    }

    @SneakyThrows
    private File saveToTempFolder(MultipartFile migrationScripts) {
        var copy = File.createTempFile(UUID.randomUUID().toString(), "-scripts.zip");
        copy(migrationScripts, copy);
        return copy;
    }

    @SneakyThrows
    private static void copy(MultipartFile migrationScripts, File copy) {
        try (var o = new FileOutputStream(copy)) {
            ByteStreams.copy(migrationScripts.getInputStream(), o);
        }
    }

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class FailedToReadEnvironmentFile extends RuntimeException {
    public FailedToReadEnvironmentFile(Exception e) {
        super(e);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class FailedToExtractZip extends RuntimeException {
    public FailedToExtractZip(Exception e) {
        super(e);
    }
}