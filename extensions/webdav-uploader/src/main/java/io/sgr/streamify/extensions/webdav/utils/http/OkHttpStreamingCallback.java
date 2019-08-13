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
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

public class OkHttpStreamingCallback implements Callback {

    private final CompletableFuture<?> future;

    public OkHttpStreamingCallback(@Nonnull final CompletableFuture<?> future) {
        //noinspection ConstantConditions
        checkArgument(nonNull(future), "Missing CompletableFuture!");
        this.future = future;
    }

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
            final String errStr = isNull(body) ? "NA" : body.string();
            final String err =
                    String.format("Failed to stream to '%s' because error code %d received! Details: %s", response.request().url(), response.code(), errStr);
            future.completeExceptionally(new IOException(err));
        }
    }

}
