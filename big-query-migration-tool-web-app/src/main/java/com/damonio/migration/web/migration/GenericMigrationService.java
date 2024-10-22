package com.damonio.migration.web.migration;


import com.damonio.migration.BigQueryMigrationConfiguration;
import com.damonio.migration.BigQueryMigrationService;
import com.damonio.template.BigQueryTemplate;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static com.damonio.migration.web.migration.GenericMigrationUtil.readConfigurationFile;
import static com.damonio.template.BigQueryTemplateUtil.generateCredentialsFileFromCompressedBase64StringCredentials;
import static com.damonio.template.BigQueryTemplateUtil.getServiceAccountCredentials;

@Slf4j
@Service
@RequiredArgsConstructor
class GenericMigrationService {

    private final Clock clock;

    public MigrationStatus migrate(String environmentFileName, String base64StringCredentials, MultipartFile migrationScripts) {
        var extractedLocation = tryExtractFile(migrationScripts);
        var bigQueryMigrationService = tryReadConfigurationFile(environmentFileName, extractedLocation);
        var credentialsFile = generateCredentialsFileFromCompressedBase64StringCredentials(base64StringCredentials);
        try {
            return migrate(credentialsFile, extractedLocation, bigQueryMigrationService);
        } catch (Exception e) {
            return new MigrationStatus("FAILED", Optional.of(e));
        } finally {
            deleteUploadedFiles(extractedLocation, credentialsFile);
        }
    }

    private static void deleteUploadedFiles(String extractedLocation, Path credentialsFile) {
        var deletedCredentialsFile = credentialsFile.toFile().delete();
        log.info("Credentials file deleted: [{}]", deletedCredentialsFile);
        var uploadedDecompressedZipDeleted = new File(extractedLocation).delete();
        log.info("Uploaded scripts deleted: [{}]", uploadedDecompressedZipDeleted);
    }

    private MigrationStatus migrate(Path credentialsFile, String extractedLocation, BigQueryMigrationConfiguration bigQueryMigrationService) {
        var serviceAccountCredentials = getServiceAccountCredentials(credentialsFile);
        new BigQueryMigrationService(clock, "file:" + extractedLocation + File.separator, new BigQueryTemplate(getBigQuery(bigQueryMigrationService, serviceAccountCredentials)), bigQueryMigrationService).migrate();
        return new MigrationStatus("SUCCESS", Optional.empty());
    }

    private static BigQuery getBigQuery(BigQueryMigrationConfiguration bigQueryMigrationService, ServiceAccountCredentials credentials) {
        return BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(bigQueryMigrationService.getProjectId())
                .build()
                .getService();
    }

    private String tryExtractFile(MultipartFile migrationScripts) {
        try {
            return extractFile(migrationScripts);
        } catch (Exception e) {
            throw new FailedToExtractZip(e);
        }
    }

    private static BigQueryMigrationConfiguration tryReadConfigurationFile(String environmentFileName, String extractedLocation) {
        try {
            return readConfigurationFile(environmentFileName, extractedLocation, "big-query-migration-configuration");
        } catch (Exception e) {
            throw new FailedToReadEnvironmentFile(e);
        }
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