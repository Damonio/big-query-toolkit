package com.damonio.migration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.threeten.bp.format.DateTimeFormatter;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationLog {
    private Integer installRank;
    private String fileName;
    private Boolean successful;
    private String checksum;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
    private LocalDateTime executionLocalDateTime;
}
