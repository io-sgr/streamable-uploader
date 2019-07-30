package io.sgr.streamable.uploader.config;

import io.sgr.streamable.uploader.streams.webdav.WebDavUploadChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChannelConfigurer {

    private final String baseUrl;
    private final String username;
    private final String password;

    @Autowired
    public ChannelConfigurer(
            @Value("${webdav.base-url}") final String baseUrl,
            @Value("${webdav.username}") final String username,
            @Value("${webdav.password}") final String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    @Bean
    public WebDavUploadChannel webDavUploadChannel() {
        return new WebDavUploadChannel(baseUrl, username, password);
    }

}
