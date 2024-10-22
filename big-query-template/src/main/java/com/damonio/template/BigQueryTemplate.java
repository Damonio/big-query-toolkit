package com.damonio.template;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BigQueryTemplate {

    private final BigQuery bigQuery;

    @SneakyThrows
    public void execute(String query) {
        executeQuery(query);
    }

    @SneakyThrows
    private TableResult executeQuery(String query) {
        var queryConfig = QueryJobConfiguration.newBuilder(query).build();
        return bigQuery.query(queryConfig);
    }

    public <T> List<T> execute(String query, Class<T> testTableClass) {
        var tableResult = executeQuery(query);
        return BigQueryMapper.toPojo(tableResult, testTableClass);
    }

    public BigQuery getBigQuery() {
        return bigQuery;
    }
}
