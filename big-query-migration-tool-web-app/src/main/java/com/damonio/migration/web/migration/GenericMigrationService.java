package com.damonio.migration.web.migration;

//import com.damonio.migration.BigQueryMigrationService;
//import com.damonio.migration.BigQueryMigrationServiceConfiguration;
//import com.damonio.template.BigQueryTemplate;

import com.damonio.migration.BigQueryMigrationService;
import com.damonio.migration.BigQueryMigrationServiceConfiguration;
import com.damonio.migration.Util;
import com.damonio.template.BigQueryTemplate;
import com.google.api.client.util.IOUtils;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.lingala.zip4j.ZipFile;
import org.springframework.stereotype.Service;
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
        var location = extractFile(migrationScripts);
        var file = Util.readFile(environmentFileName);
        new BigQueryMigrationService(clock, new BigQueryTemplate(null), BigQueryMigrationServiceConfiguration.builder().scriptLocation(location).build()).migrate();
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
