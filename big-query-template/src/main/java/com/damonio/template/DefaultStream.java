package com.damonio.template;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.Exceptions;
import com.google.cloud.bigquery.storage.v1.Exceptions.AppendSerializationError;
import com.google.cloud.bigquery.storage.v1.Exceptions.MaximumRequestCallbackWaitTimeExceededException;
import com.google.cloud.bigquery.storage.v1.Exceptions.StorageException;
import com.google.cloud.bigquery.storage.v1.Exceptions.StreamWriterClosedException;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.threeten.bp.Duration;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import static com.damonio.template.BigQueryMapper.toJSON;


/**
 * TODO Improve this component, I had a miss understanding that the BigQuery Emulator had the Storage Write API implemented.
 */
@RequiredArgsConstructor
public class DefaultStream {

    /**
     * TODO Find out how to close the streams:
     * Remember to close streams on a pod shutdown. Failing to do so, especially during mass pod shutdowns (e.g., during deployments),
     * can lead to delays in closing connections. If new connections are created simultaneously for new pods, this can result in reaching connection Quotas.
     * https://medium.com/@bravnic/bigquery-storage-write-api-at-scale-7affcc2d7a93
     */
    private final BigQueryWriteClient bigQueryWriteClient;

    // TODO make it good!.
    @SneakyThrows
    public <T> void stream(List<T> bigQueryEntities) {
        var projectId = "test_project";
        var datasetName = "test_dataset";

        var parentTable = TableName.of(projectId, datasetName, getTableName(bigQueryEntities));
        var writer = new DataWriter();
        writer.initialize(parentTable, bigQueryWriteClient);
        //TODO check how to calculate the size of this payload and batch it in chunks smaller than 10 MB
        // https://cloud.google.com/bigquery/quotas#write-api-limits
        var payload = new JSONArray(toJSON(bigQueryEntities));
        writer.append(new AppendContext(payload), bigQueryWriteClient);
        writer.cleanup();
    }

    private <T> String getTableName(List<T> bigQueryEntities) {
        var name = bigQueryEntities.get(0).getClass().getName();
        return Util.toSnakeCase(name);
    }

    private static class AppendContext {
        JSONArray data;

        AppendContext(JSONArray data) {
            this.data = data;
        }
    }

    private static class DataWriter {
        private static final int MAX_RECREATE_COUNT = 3;
        private final Phaser inflightRequestCount = new Phaser(1);
        private final Object lock = new Object();
        private JsonStreamWriter streamWriter;

        @GuardedBy("lock")
        private RuntimeException error = null;

        private AtomicInteger recreateCount = new AtomicInteger(0);

        private JsonStreamWriter createStreamWriter(String tableName, BigQueryWriteClient bigQueryWriteClient) {
            return getJsonStreamWriter(tableName, bigQueryWriteClient);
        }

        /**
         * Configure in-stream automatic retry settings.
         * Error codes that are immediately retried:
         * * ABORTED, UNAVAILABLE, CANCELLED, INTERNAL, DEADLINE_EXCEEDED
         * Error codes that are retried with exponential backoff:
         * * RESOURCE_EXHAUSTED
         */
        private RetrySettings retrySettings() {
            return RetrySettings.newBuilder().setInitialRetryDelay(Duration.ofMillis(500)).setRetryDelayMultiplier(1.1).setMaxAttempts(5).setMaxRetryDelay(Duration.ofMinutes(1)).build();
        }

        /**
         * Use the JSON stream writer to send records in JSON format. Specify the table name to write
         * to the default stream.
         * For more information about JsonStreamWriter, see:
         * https://googleapis.dev/java/google-cloud-bigquerystorage/latest/com/google/cloud/bigquery/storage/v1/JsonStreamWriter.html
         * If value is missing in json and there is a default value configured on bigquery
         * column, apply the default value to the missing value field.
         */
        @SneakyThrows
        private JsonStreamWriter getJsonStreamWriter(String tableName, BigQueryWriteClient bigQueryWriteClient) {
            return JsonStreamWriter
                    .newBuilder(tableName, bigQueryWriteClient)
//                    .setExecutorProvider(FixedExecutorProvider.create(Executors.newScheduledThreadPool(100))).setChannelProvider(BigQueryWriteSettings.defaultGrpcTransportProviderBuilder().setKeepAliveTime(Duration.ofMinutes(1)).setKeepAliveTimeout(Duration.ofMinutes(1)).setKeepAliveWithoutCalls(true).setChannelsPerCpu(2).build()).setEnableConnectionPool(true)
//                    .setDefaultMissingValueInterpretation(AppendRowsRequest.MissingValueInterpretation.DEFAULT_VALUE).setRetrySettings(retrySettings())
                    .setCredentialsProvider(new NoCredentialsProvider())
                    .setEndpoint("0.0.0.0:9060")
                    .build();
        }

        public void initialize(TableName parentTable, BigQueryWriteClient bigQueryWriteClient) {
            streamWriter = createStreamWriter(parentTable.toString(), bigQueryWriteClient);
        }

        public void append(AppendContext appendContext, BigQueryWriteClient bigQueryWriteClient) throws DescriptorValidationException, IOException, InterruptedException {
            synchronized (this.lock) {
                if (!streamWriter.isUserClosed() && streamWriter.isClosed() && recreateCount.getAndIncrement() < MAX_RECREATE_COUNT) {
                    streamWriter = createStreamWriter(streamWriter.getStreamName(), bigQueryWriteClient);
                    this.error = null;
                }
                // If earlier appends have failed, we need to reset before continuing.
                if (this.error != null) {
                    throw this.error;
                }
            }
            // Append asynchronously for increased throughput.
            ApiFuture<AppendRowsResponse> future = streamWriter.append(appendContext.data);
            ApiFutures.addCallback(future, new AppendCompleteCallback(this, appendContext, bigQueryWriteClient), MoreExecutors.directExecutor());

            // Increase the count of in-flight requests.
            inflightRequestCount.register();
        }

        public void cleanup() {
            inflightRequestCount.arriveAndAwaitAdvance();
            streamWriter.close();

            synchronized (this.lock) {
                if (this.error != null) {
                    throw this.error;
                }
            }
        }

        @RequiredArgsConstructor
        static class AppendCompleteCallback implements ApiFutureCallback<AppendRowsResponse> {

            private final DataWriter parent;
            private final AppendContext appendContext;
            private final BigQueryWriteClient bigQueryWriteClient;

            public void onSuccess(AppendRowsResponse response) {
                System.out.format("Append success\n");
                this.parent.recreateCount.set(0);
                reduceCountOfInflightRequest();
            }

            public void onFailure(Throwable throwable) {
                if (throwable instanceof AppendSerializationError) {
                    AppendSerializationError ase = (AppendSerializationError) throwable;
                    Map<Integer, String> rowIndexToErrorMessage = ase.getRowIndexToErrorMessage();
                    if (rowIndexToErrorMessage.size() > 0) {
                        // Omit the faulty rows
                        JSONArray dataNew = new JSONArray();
                        for (int i = 0; i < appendContext.data.length(); i++) {
                            if (!rowIndexToErrorMessage.containsKey(i)) {
                                dataNew.put(appendContext.data.get(i));
                            } else {
                                // process faulty rows by placing them on a dead-letter-queue, for instance
                            }
                        }

                        // Retry the remaining valid rows, but using a separate thread to
                        // avoid potentially blocking while we are in a callback.
                        if (dataNew.length() > 0) {
                            try {
                                this.parent.append(new AppendContext(dataNew), bigQueryWriteClient);
                            } catch (DescriptorValidationException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        // Mark the existing attempt as done since we got a response for it
                        reduceCountOfInflightRequest();
                        return;
                    }
                }

                boolean resendRequest = false;
                if (throwable instanceof MaximumRequestCallbackWaitTimeExceededException) {
                    resendRequest = true;
                } else if (throwable instanceof StreamWriterClosedException) {
                    if (!parent.streamWriter.isUserClosed()) {
                        resendRequest = true;
                    }
                }
                if (resendRequest) {
                    // Retry this request.
                    try {
                        this.parent.append(new AppendContext(appendContext.data), bigQueryWriteClient);
                    } catch (DescriptorValidationException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // Mark the existing attempt as done since we got a response for it
                    reduceCountOfInflightRequest();
                    return;
                }

                synchronized (this.parent.lock) {
                    if (this.parent.error == null) {
                        StorageException storageException = Exceptions.toStorageException(throwable);
                        this.parent.error = (storageException != null) ? storageException : new RuntimeException(throwable);
                    }
                }
                reduceCountOfInflightRequest();
            }

            private void reduceCountOfInflightRequest() {
                this.parent.inflightRequestCount.arriveAndDeregister();
            }
        }
    }
}
