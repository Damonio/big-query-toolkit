package com.damonio.template;

import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.services.bigquery.Bigquery;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.storage.v1.AppendRowsRequest;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.common.base.CaseFormat;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.threeten.bp.Duration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static com.damonio.template.Util.toSnakeCase;

@Slf4j
@RequiredArgsConstructor
public class BigQueryTemplate {

    private final BigQuery bigQuery;

    public <T> void stream(T bigQueryEntity) {
        stream(List.of(bigQueryEntity));
    }

    @SneakyThrows
    public <T> void stream(List<T> bigQueryEntities) {
        if (CollectionUtils.isEmpty(bigQueryEntities)) return;
        var retrySettings = getRetrySettings();

        BigQueryWriteClient.create(BigQueryWriteSettings.newBuilder(ClientContext.newBuilder().setEndpoint().build())


        // Use the JSON stream writer to send records in JSON format. Specify the table name to write
        // to the default stream.
        // For more information about JsonStreamWriter, see:
        // https://googleapis.dev/java/google-cloud-bigquerystorage/latest/com/google/cloud/bigquery/storage/v1/JsonStreamWriter.html
        JsonStreamWriter.newBuilder(getTableName(bigQueryEntities), bigQuery)
                .setExecutorProvider(FixedExecutorProvider.create(Executors.newScheduledThreadPool(100)))
                .setChannelProvider(
                        BigQueryWriteSettings.defaultGrpcTransportProviderBuilder()
                                .setKeepAliveTime(org.threeten.bp.Duration.ofMinutes(1))
                                .setKeepAliveTimeout(org.threeten.bp.Duration.ofMinutes(1))
                                .setKeepAliveWithoutCalls(true)
                                .setChannelsPerCpu(2)
                                .build())
                .setEnableConnectionPool(true)
                // If value is missing in json and there is a default value configured on bigquery
                // column, apply the default value to the missing value field.
                .setDefaultMissingValueInterpretation(
                        AppendRowsRequest.MissingValueInterpretation.DEFAULT_VALUE)
                .setRetrySettings(retrySettings)
                .build();
    }

    private static RetrySettings getRetrySettings() {
        return RetrySettings.newBuilder()
                .setInitialRetryDelay(Duration.ofMillis(500))
                .setRetryDelayMultiplier(1.1)
                .setMaxAttempts(5)
                .setMaxRetryDelay(Duration.ofMinutes(1))
                .build();
    }

    private static <T> String getTableName(List<T> bigQueryEntities) {
        var className = bigQueryEntities.get(0).getClass().getName();
        return toSnakeCase(className);
    }

    @SneakyThrows
    public void execute(String query) {
        executeQuery(query);
    }

    @SneakyThrows
    public void executeQuery(String query) {
        var queryConfig = QueryJobConfiguration.newBuilder(query).build();
        var result = bigQuery.query(queryConfig);
        log.info("{}", result);
    }

    public <T> List<T> execute(String query, Class<T> testTableClass) {
        return null;
    }
}
