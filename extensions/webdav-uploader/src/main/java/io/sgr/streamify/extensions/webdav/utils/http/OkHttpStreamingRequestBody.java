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

package io.sgr.streamify.extensions.webdav.utils.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.nonNull;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OkHttpStreamingRequestBody extends RequestBody {

    private final InputStream inputStream;
    private final String contentType;

    public OkHttpStreamingRequestBody(@Nonnull final InputStream inputStream, @Nullable final String contentType) {
        //noinspection ConstantConditions
        checkArgument(nonNull(inputStream), "Missing input stream!");
        this.inputStream = inputStream;
        this.contentType = contentType;
    }

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

    @Override
    public boolean isOneShot() {
        return true;
    }

}
