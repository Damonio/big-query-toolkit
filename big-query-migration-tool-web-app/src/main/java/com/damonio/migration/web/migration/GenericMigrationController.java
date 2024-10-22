package com.damonio.migration.web.migration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/migrate")
class GenericMigrationController {

    private final GenericMigrationService genericMigrationService;

    @PostMapping
    void migrate(@RequestParam("migrationRequest") String migrationRequest, @RequestParam("migrationScripts") MultipartFile migrationScripts) {
        System.out.println();
//        genericMigrationService.migrate(migrationRequest, migrationScripts);
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
