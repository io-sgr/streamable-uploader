package io.sgr.streamable.uploader;

import java.util.Optional;

import javax.annotation.Nonnull;

public interface StreamingRequest {

    @Nonnull
    String getId();

    @Nonnull
    String getContentType();

    Optional<Integer> getBufferSize();

}
