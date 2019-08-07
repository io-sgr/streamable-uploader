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
import static io.sgr.streamify.extensions.webdav.utils.WebDavConstants.DEFAULT_BUFFER_SIZE;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.sgr.streamify.StreamingChannel;
import io.sgr.streamify.StreamingRequest;
import io.sgr.streamify.extensions.webdav.utils.http.BasicAuthInterceptor;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public class WebDavUploadChannel implements StreamingChannel {

    private static final String WEB_PATH_SEPARATOR = "/";

    private final String baseUrl;
    private final OkHttpClient client;

    public WebDavUploadChannel(@Nonnull final String baseUrl, final String username, final String password) {
        checkArgument(!isNullOrEmpty(baseUrl), "Missing base URL!");
        this.baseUrl = baseUrl.endsWith(WEB_PATH_SEPARATOR) ? baseUrl : baseUrl + WEB_PATH_SEPARATOR;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (!isNullOrEmpty(username)) {
            builder.addInterceptor(new BasicAuthInterceptor(username, password));
        }
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(1, TimeUnit.HOURS);
        this.client = builder.build();
    }

    @SuppressFBWarnings("OS_OPEN_STREAM")   // The returned stream will be closed somewhere else.
    @Nonnull
    @Override
    public OutputStream open(@Nonnull final StreamingRequest request, @Nonnull final String identifier) throws IOException {
        final String relPath = buildRelativePath(request, identifier);
        final OkHttpClient client = rebuildClientPerRequestIfNeeded(request);
        return new WebDavOutputStream(client, baseUrl + relPath, request.getContentType(), request.getBufferSize().orElse(DEFAULT_BUFFER_SIZE))
                .init();
    }

    private String buildRelativePath(@Nonnull final StreamingRequest request, @Nonnull final String identifier) {
        // TODO: Basically calculate file name, change extension based on request if needed, etc.
        return identifier.startsWith(WEB_PATH_SEPARATOR) ? identifier.substring(WEB_PATH_SEPARATOR.length(), identifier.length() - 1) : identifier;
    }

    private OkHttpClient rebuildClientPerRequestIfNeeded(final StreamingRequest request) {
        // TODO: This will be extremely useful when need to set per request timeout.
        //        For example:
        //        return this.client.newBuilder()
        //                .connectTimeout(30, TimeUnit.SECONDS)
        //                .readTimeout(1, TimeUnit.HOURS)
        //                .build();
        return this.client;
    }

}
