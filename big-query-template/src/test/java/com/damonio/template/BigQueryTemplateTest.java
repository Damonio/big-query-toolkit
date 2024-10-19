package com.damonio.template;

import lombok.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigQueryTemplateTest extends BaseTest {

    @Autowired private BigQueryTemplate bigQueryTemplate;

    @BeforeEach
    void setUp() {
        bigQueryTemplate.execute("CREATE TABLE IF NOT EXISTS `test_dataset.test_table` (int_column INT64, string_column STRING)");
    }

    @Test
    @SneakyThrows
    void canStreamData() {
        var testTable = TestTable.builder().intColumn(1).stringColumn("string").build();
        bigQueryTemplate.stream(testTable);

        var fromBigQuery = bigQueryTemplate.execute("SELECT * FROM `test_dataset.test_table`", TestTable.class);

        assertEquals(List.of(testTable), fromBigQuery);

    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TestTable {
    private int intColumn;
    private String stringColumn;
}