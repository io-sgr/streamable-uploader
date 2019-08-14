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
import static java.util.Objects.nonNull;

import io.sgr.streamify.OutputStreamWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;

public class GZipOutputStreamWrapper implements OutputStreamWrapper {

    @Nonnull
    @Override
    public OutputStream wrap(@Nonnull final OutputStream outputStream) throws IOException {
        //noinspection ConstantConditions
        checkArgument(nonNull(outputStream), "Missing output stream!");
        return new GZIPOutputStream(outputStream);
    }

}
