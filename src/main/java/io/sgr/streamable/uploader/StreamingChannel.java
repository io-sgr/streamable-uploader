package io.sgr.streamable.uploader;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;

public interface StreamingChannel {

    @Nonnull
    OutputStream open(@Nonnull StreamingRequest request, @Nonnull String identifier) throws IOException;

}
