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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class SingleFileZipOutputStreamWrapperTest {

    @Test
    public void testZip() throws IOException {
        SingleFileZipOutputStreamWrapper wrapper = new SingleFileZipOutputStreamWrapper("file.txt");
        final byte[] bytes = generateLargeBytes();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        try (
                OutputStream outputStream = wrapper.wrap(bos)
        ) {
            outputStream.write(bytes);
        }
        assertTrue(bos.size() < bytes.length);
    }

    private static byte[] generateLargeBytes() {
        StringBuilder rawText = new StringBuilder();
        int round = 10000;
        while (round > 0) {
            rawText.append(UUID.randomUUID().toString());
            --round;
        }
        return rawText.toString().getBytes();
    }

}