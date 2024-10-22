package com.damonio.migration.web.migration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationStatus {
    private String status;
    private Optional<Exception> exception;
}
