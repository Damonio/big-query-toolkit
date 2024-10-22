package com.damonio.migration.web.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/migrate")
class GenericMigrationController {

    private final GenericMigrationService genericMigrationService;

    @PostMapping
    void migrate(@RequestParam("migrationRequest") String environment, @RequestParam("migrationRequest") String credentials, @RequestParam("migrationScripts") MultipartFile migrationScripts) {
        genericMigrationService.migrate(environment, credentials, migrationScripts);
    }

}