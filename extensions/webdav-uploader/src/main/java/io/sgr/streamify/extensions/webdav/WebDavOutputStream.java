/*
 * Copyright 2017-2019 SgrAlpha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sgr.streamify.extensions.webdav;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.nonNull;

import io.sgr.streamify.OutputStreamWrapper;
import io.sgr.streamify.extensions.webdav.utils.WebDavConstants;
import io.sgr.streamify.extensions.webdav.utils.http.OkHttpStreamingCallback;
import io.sgr.streamify.extensions.webdav.utils.http.OkHttpStreamingRequestBody;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

public class WebDavOutputStream extends OutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDavOutputStream.class);

    private final OkHttpClient client;
    private final String url;
    private final String contentType;
    private final int bufferSize;
    private final Collection<OutputStreamWrapper> wrappers;

    private PipedInputStream inputStream;
    private OutputStream outputStream;
    private CompletableFuture<?> future;

    WebDavOutputStream(
            @Nonnull final OkHttpClient client, @Nonnull final String url, @Nonnull final String contentType, final int bufferSize,
            final Collection<OutputStreamWrapper> wrappers) {
        //noinspection ConstantConditions
        checkArgument(nonNull(client), "Missing okhttp client!");
        this.client = client;
        checkArgument(!isNullOrEmpty(url), "Missing target URL to upload to!");
        this.url = url;
        checkArgument(!isNullOrEmpty(contentType), "Missing content type!");
        this.contentType = contentType;
        this.bufferSize = bufferSize <= 0 ? WebDavConstants.DEFAULT_BUFFER_SIZE : bufferSize;
        this.wrappers = wrappers;
    }

    WebDavOutputStream init() throws IOException {
        this.inputStream = new PipedInputStream(bufferSize);
        this.outputStream = new PipedOutputStream(inputStream);
        if (nonNull(this.wrappers) && !wrappers.isEmpty()) {
            for (OutputStreamWrapper wrapper : wrappers) {
                this.outputStream = wrapper.wrap(this.outputStream);
            }
        }
        final RequestBody reqBody = new OkHttpStreamingRequestBody(inputStream, contentType);
        LOGGER.info("Uploading content to '{}'", url);
        final Request request = new Request.Builder().url(url).put(reqBody).build();
        this.future = new CompletableFuture<>();
        client.newCall(request).enqueue(new OkHttpStreamingCallback(future));
        return this;
    }

    public void write(final int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void close() throws IOException {
        try {
            outputStream.close();
            if (nonNull(future)) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e);
        } finally {
            inputStream.close();
        }
        LOGGER.info("Completed.");
    }

}
