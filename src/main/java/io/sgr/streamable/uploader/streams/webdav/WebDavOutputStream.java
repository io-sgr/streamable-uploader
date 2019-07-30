package io.sgr.streamable.uploader.streams.webdav;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WebDavOutputStream extends OutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDavOutputStream.class);

    private final OkHttpClient client;
    private final String url;
    private final String contentType;

    private final PipedInputStream inputStream;
    private final PipedOutputStream outputStream;

    private CompletableFuture<?> future;

    WebDavOutputStream(
            @Nonnull final OkHttpClient client, @Nonnull final String url,
            @Nonnull final String contentType, final int bufferSize
    ) throws IOException {
        checkArgument(nonNull(client), "Missing okhttp client!");
        this.client = client;
        checkArgument(!isNullOrEmpty(url), "Missing target URL to upload to!");
        this.url = url;
        checkArgument(!isNullOrEmpty(contentType), "Missing content type!");
        this.contentType = contentType;
        this.inputStream = new PipedInputStream(bufferSize <= 0 ? WebDavConstants.DEFAULT_BUFFER_SIZE : bufferSize);
        this.outputStream = new PipedOutputStream(this.inputStream);
    }

    WebDavOutputStream init() {
        final RequestBody reqBody = buildBodyFromInputStream(inputStream, contentType);
        LOGGER.info("Uploading content to '{}'", url);
        final Request request = new Request.Builder().url(url).put(reqBody).build();
        this.future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@Nonnull final Call call, @Nonnull final IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@Nonnull final Call call, @Nonnull final Response response) throws IOException {
                try (
                        ResponseBody body = response.body()
                ) {
                    if (response.isSuccessful()) {
                        future.complete(null);
                        return;
                    }
                    final String errStr = isNull(body) ? null : body.string();
                    final String err = String.format("Failed to upload to '%s' because error code %d received! Details: %s", url, response.code(), errStr);
                    future.completeExceptionally(new IOException(err));
                }
            }
        });
        return this;
    }

    public void write(final int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
        try {
            if (nonNull(future)) {
                future.get();
            }
            inputStream.close();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e);
        }
        LOGGER.info("Completed.");
    }

    private RequestBody buildBodyFromInputStream(@Nonnull final InputStream inputStream, final String contentType) {
        return new RequestBody() {

            @Nullable
            @Override
            public MediaType contentType() {
                return isNullOrEmpty(contentType) ? null : MediaType.parse(contentType);
            }

            @Override
            public void writeTo(@Nonnull final BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(inputStream);
                    sink.writeAll(source);
                } finally {
                    Util.closeQuietly(source);
                }
            }

        };
    }

}
