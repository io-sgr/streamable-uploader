package io.sgr.streamable.uploader.utils.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import javax.annotation.Nonnull;

public class BasicAuthInterceptor implements Interceptor {

    private final String credentials;

    public BasicAuthInterceptor(final String username, final String password) {
        checkArgument(!isNullOrEmpty(username), "Missing username!");
        checkArgument(!isNullOrEmpty(password), "Missing password!");
        credentials = Credentials.basic(username, password, UTF_8);
    }

    @Nonnull
    @Override
    public Response intercept(@Nonnull final Chain chain) throws IOException {
        final Request authenticatedRequest = chain.request().newBuilder().header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

}
