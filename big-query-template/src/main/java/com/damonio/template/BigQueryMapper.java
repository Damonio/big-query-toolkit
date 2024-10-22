package com.damonio.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@UtilityClass
public class BigQueryMapper {

    public static <T> List<T> toPojo(TableResult tableResult, Class<T> testTableClass) {
        var schema = tableResult.getSchema().getFields().parallelStream().toList();
        return StreamSupport.stream(tableResult.getValues().spliterator(), true).map(row -> mapRow(schema, row, testTableClass)).toList();
    }

    private static <T> T mapRow(List<Field> schema, FieldValueList row, Class<T> testTableClass) {
        var asMap = schema.parallelStream()
                .map(field -> new Column(field.getName(), row.get(field.getName()).getValue()))
                .collect(Collectors.toMap(Column::getName, Column::getValue));
        return snakeCaseMapper().convertValue(asMap, testTableClass);
    }

    @Data
    @AllArgsConstructor
    private class Column {
        private String name;
        private Object value;
    }

    private static ObjectMapper snakeCaseMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return objectMapper;
    }
}
