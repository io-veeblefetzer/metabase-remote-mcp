package io.veeblefetzer.remote_mcp_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * Configuration properties for the Metabase API client.
 * These properties can be configured via application.properties or environment variables.
 */
@ConfigurationProperties(prefix = "metabase")
@Validated
public class MetabaseProperties {

    /**
     * Base URL of the Metabase instance (e.g., http://localhost:3000)
     */
    @NotBlank(message = "Metabase URL is required")
    private String url;

    /**
     * API key for authentication with Metabase.
     * Can also be a session token.
     */
    @NotBlank(message = "Metabase API key is required")
    private String apiKey;

    /**
     * Connection timeout for HTTP requests.
     * Default: 10 seconds
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Read timeout for HTTP requests.
     * Default: 30 seconds
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * Maximum number of connections in the pool.
     * Default: 10
     */
    private int maxConnections = 10;

    // Getters and Setters

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
}
