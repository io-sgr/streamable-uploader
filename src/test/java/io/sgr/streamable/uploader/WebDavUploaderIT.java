package io.sgr.streamable.uploader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import io.sgr.streamable.uploader.config.ChannelConfigurer;
import io.sgr.streamable.uploader.requests.JsonLinesBasedStreamRequest;
import io.sgr.streamable.uploader.streams.webdav.WebDavUploadChannel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@TestPropertySource(locations = {
        "classpath:application-test.properties"
})
@ContextConfiguration(classes = {
        ChannelConfigurer.class
})
@RunWith(SpringRunner.class)
public class WebDavUploaderIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDavUploaderIT.class);

    /**
     * See {@link ChannelConfigurer} to find out how this upload channel been configured.
     */
    @Autowired
    private WebDavUploadChannel channel;

    /**
     * You can use -Xmx to set heap to a relatively low value like 256M, then upload a file over 2GB.
     * Any profiling tool(like VisualVM) can be used during the process.
     *
     * @throws IOException
     *         If anything goes wrong.
     */
    @Test
    public void testUploadLargeFile() throws IOException {
        final String largeFile = Optional.ofNullable(System.getenv("LARGE_FILE_FOR_TEST"))
                .orElseThrow(() -> new IOException("In order to run this test, set LARGE_FILE_FOR_TEST in environment variable!"));
        // Read lines from a large file in a blocking way.
        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(largeFile))
        ) {
            // Magic begins here.
            final StreamingRequest request = new JsonLinesBasedStreamRequest()
                    .setBufferSize(null)    // This is only a demonstration, set to null or negative value will use default buffer size.
                    .setCompression(false); // Disable compression.
            try (
                    OutputStream outputStream = channel.open(request, request.getId())
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputStream.write(line.getBytes(UTF_8));   // Simply write line to the output stream after it's been read.
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

}
