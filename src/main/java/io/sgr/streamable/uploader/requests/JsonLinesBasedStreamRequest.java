package io.sgr.streamable.uploader.requests;

import io.sgr.streamable.uploader.StreamingRequest;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

public class JsonLinesBasedStreamRequest implements StreamingRequest {

    private static final String DEFAULT_CONTENT_TYPE = "application/x-jsonlines";
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;

    private final String id;

    private boolean compress;
    private Integer bufferSize;

    public JsonLinesBasedStreamRequest() {
        id = UUID.randomUUID().toString();
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public String getContentType() {
        return DEFAULT_CONTENT_TYPE;
    }

    @Override
    public Optional<Integer> getBufferSize() {
        return Optional.ofNullable(bufferSize);
    }

    public JsonLinesBasedStreamRequest setBufferSize(final Integer bufferSize) {
        this.bufferSize = Optional.ofNullable(bufferSize).filter(size -> size > 0).orElse(DEFAULT_BUFFER_SIZE);
        return this;
    }

    public boolean isCompress() {
        return compress;
    }

    public JsonLinesBasedStreamRequest setCompression(final boolean compress) {
        this.compress = compress;
        return this;
    }
}
