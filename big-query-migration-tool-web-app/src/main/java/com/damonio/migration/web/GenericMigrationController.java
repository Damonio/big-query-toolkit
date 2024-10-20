package com.damonio.migration.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/migrate")
class GenericMigrationController {

    private final GenericMigrationService genericMigrationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void migrate(@RequestPart MigrationRequest migrationRequest, @RequestPart("file") MultipartFile migrationScripts) {
        genericMigrationService.migrate(migrationRequest, migrationScripts);
    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MigrationRequest {
    private String projectId;
    private String datasetId;
    private String credentials;
    private Map<String, String> replacements = new HashMap<>();
}
