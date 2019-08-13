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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RunWith(MockitoJUnitRunner.class)
public class OkHttpStreamingCallbackTest {

    @Mock
    private CompletableFuture<String> future;
    @Mock
    private Call call;

    @Test
    public void testOkHttpCallbackSuccessful() throws IOException {
        final Request request = new Request.Builder().url("http://locahost/").build();
        final Response response = new Response.Builder().protocol(Protocol.HTTP_2).code(200).message("OK").request(request).body(null).build();

        OkHttpStreamingCallback callback = new OkHttpStreamingCallback(future);
        callback.onResponse(call, response);

        verify(future, times(1)).complete(eq(null));
    }

    @Test
    public void testOkHttpCallbackUnsuccessfulWithoutBody() throws IOException {
        final Request request = new Request.Builder().url("http://locahost/").build();
        final Response response = new Response.Builder().protocol(Protocol.HTTP_2).code(401).message("UNAUTHORIZED").request(request).body(null).build();

        OkHttpStreamingCallback callback = new OkHttpStreamingCallback(future);
        callback.onResponse(call, response);

        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(future, times(1)).completeExceptionally(captor.capture());
        final Throwable t = captor.getValue();
        assertNotNull(t);
        assertTrue(t instanceof IOException);
        final String err = String.format("Failed to stream to '%s' because error code %d received! Details: %s", "http://locahost/", 401, "NA");
        assertEquals(err, t.getMessage());
    }

    @Test
    public void testOkHttpCallbackUnsuccessfulWithBody() throws IOException {
        final Request request = new Request.Builder().url("http://locahost/").build();
        final ResponseBody body = ResponseBody.create(MediaType.parse("text/plain"), "some_content");
        final Response response = new Response.Builder().protocol(Protocol.HTTP_2).code(404).message("NOT_FOUND").request(request).body(body).build();

        OkHttpStreamingCallback callback = new OkHttpStreamingCallback(future);
        callback.onResponse(call, response);

        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(future, times(1)).completeExceptionally(captor.capture());
        final Throwable t = captor.getValue();
        assertNotNull(t);
        assertTrue(t instanceof IOException);
        final String err = String.format("Failed to stream to '%s' because error code %d received! Details: %s", "http://locahost/", 404, "some_content");
        assertEquals(err, t.getMessage());
    }

    @Test
    public void testOkHttpCallbackFailure() {
        OkHttpStreamingCallback callback = new OkHttpStreamingCallback(future);
        callback.onFailure(call, new IOException("some_ioe"));

        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(future, times(1)).completeExceptionally(captor.capture());
        final Throwable t = captor.getValue();
        assertTrue(t instanceof IOException);
        assertEquals("some_ioe", t.getMessage());
    }

}