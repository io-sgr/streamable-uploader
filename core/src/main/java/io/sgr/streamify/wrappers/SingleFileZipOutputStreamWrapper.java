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

package io.sgr.streamify.wrappers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.nonNull;

import io.sgr.streamify.OutputStreamWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

public class SingleFileZipOutputStreamWrapper implements OutputStreamWrapper {

    private final String entryName;

    public SingleFileZipOutputStreamWrapper(final String entryName) {
        checkArgument(!isNullOrEmpty(entryName), "Missing entry name!");
        this.entryName = entryName;
    }

    @Override
    @Nonnull
    public OutputStream wrap(@Nonnull final OutputStream outputStream) throws IOException {
        //noinspection ConstantConditions
        checkArgument(nonNull(outputStream), "Missing output stream!");
        return new SingleEntryZipOutputStream(outputStream, entryName);
    }

    private static class SingleEntryZipOutputStream extends OutputStream {

        private final ZipOutputStream delegate;

        private SingleEntryZipOutputStream(final OutputStream outputStream, final String entryName) throws IOException {
            delegate = new ZipOutputStream(outputStream);
            delegate.putNextEntry(new ZipEntry(entryName));
        }

        @Override
        public void write(final int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(@Nonnull final byte[] b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(@Nonnull final byte[] b, final int off, final int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.closeEntry();
            } finally {
                delegate.close();
            }
        }
    }

}
