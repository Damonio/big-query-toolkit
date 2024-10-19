package com.damonio.template;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BigQueryTemplate {

    private final BigQuery bigQuery;

    public void stream(Object bigQueryEntity) {
        stream(List.of(bigQueryEntity));
    }

    public void stream(List<?> bigQueryEntities) {

    }

    @SneakyThrows
    public void execute(String query) {
        var queryConfig = QueryJobConfiguration.newBuilder(query).build();
        var result = bigQuery.query(queryConfig);
        log.info("{}", result);
    }
}
