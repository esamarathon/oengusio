package app.oengus.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "oengus")
public class OengusConfiguration {
    private String baseUrl;
    private List<String> oauthOrigins;
    private String shortUrl;
    private int pageSize;

    private JwtSettings jwt;

    @Getter
    @Setter
    public static class JwtSettings {
        private String secret;
        private long expiration; // 604800 seconds == 7 days
    }
}
