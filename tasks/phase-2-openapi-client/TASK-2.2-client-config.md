# Task 2.2: Client Configuration Class

**Task ID:** 2.2  
**Phase:** 2 - OpenAPI Client Generation  
**GitHub Issue:** [#2](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/2)  
**Status:** ⬜ Not Started

---

## Objective

Create a Spring configuration class that sets up the generated Metabase API client with proper authentication, timeout settings, and error handling.

---

## Prerequisites

- Task 2.1 completed (Maven plugin configuration)
- Generated API client code exists and compiles
- Understanding of Spring WebClient configuration

---

## Technical Details

### Files to Create

1. **Configuration Class:** `src/main/java/io/veeblefetzer/remote_mcp_server/config/MetabaseClientConfig.java`
2. **Properties Class:** `src/main/java/io/veeblefetzer/remote_mcp_server/config/MetabaseProperties.java`
3. **Update:** `src/main/resources/application.properties`

### 1. MetabaseProperties Class

```java
package io.veeblefetzer.remote_mcp_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

@ConfigurationProperties(prefix = "metabase")
@Validated
public class MetabaseProperties {

    /**
     * Base URL of the Metabase instance (e.g., http://localhost:3000)
     */
    @NotBlank
    private String url;

    /**
     * API key for authentication with Metabase
     */
    @NotBlank
    private String apiKey;

    /**
     * Connection timeout for HTTP requests
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Read timeout for HTTP requests
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * Maximum number of connections in the pool
     */
    private int maxConnections = 10;

    // Getters and setters...
}
```

### 2. MetabaseClientConfig Class

```java
package io.veeblefetzer.remote_mcp_server.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.veeblefetzer.metabase.client.ApiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(MetabaseProperties.class)
public class MetabaseClientConfig {

    private final MetabaseProperties properties;

    public MetabaseClientConfig(MetabaseProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WebClient metabaseWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                    (int) properties.getConnectTimeout().toMillis())
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(
                    properties.getReadTimeout().toMillis(), 
                    TimeUnit.MILLISECONDS)));

        return WebClient.builder()
            .baseUrl(properties.getUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-Metabase-Session", properties.getApiKey())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .filter(handleErrors())
            .build();
    }

    @Bean
    public ApiClient metabaseApiClient(WebClient metabaseWebClient) {
        ApiClient apiClient = new ApiClient(metabaseWebClient);
        apiClient.setBasePath(properties.getUrl());
        return apiClient;
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            // Log request details (implement proper logging)
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            // Log response details (implement proper logging)
            return Mono.just(response);
        });
    }

    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                // Handle error responses
                return response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(
                        new MetabaseApiException(
                            response.statusCode().value(), 
                            body)));
            }
            return Mono.just(response);
        });
    }
}
```

### 3. Exception Class

```java
package io.veeblefetzer.remote_mcp_server.config;

public class MetabaseApiException extends RuntimeException {
    
    private final int statusCode;
    private final String responseBody;

    public MetabaseApiException(int statusCode, String responseBody) {
        super("Metabase API error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
```

### 4. Application Properties

```properties
# Metabase Configuration
metabase.url=http://localhost:3000
metabase.api-key=${METABASE_API_KEY:your-api-key-here}
metabase.connect-timeout=10s
metabase.read-timeout=30s
metabase.max-connections=10
```

---

## Implementation Checklist

- [ ] Create `config` package if not exists
- [ ] Create `MetabaseProperties.java` with all properties
- [ ] Add validation annotations to required properties
- [ ] Create `MetabaseClientConfig.java` configuration class
- [ ] Configure WebClient with timeouts and headers
- [ ] Add request/response logging filters
- [ ] Create error handling filter
- [ ] Create `MetabaseApiException.java` exception class
- [ ] Update `application.properties` with Metabase settings
- [ ] Add `@EnableConfigurationProperties` annotation
- [ ] Test: Application context loads without errors
- [ ] Test: Properties are correctly bound
- [ ] Test: WebClient bean is created successfully

---

## Acceptance Criteria

- [ ] `MetabaseProperties` class exists with all configuration options
- [ ] Properties are validated (url and apiKey required)
- [ ] `MetabaseClientConfig` provides WebClient and ApiClient beans
- [ ] Timeout settings are configurable via properties
- [ ] Error handling is implemented for API failures
- [ ] Application starts without configuration errors
- [ ] Properties can be overridden via environment variables
- [ ] Logging is enabled for API requests/responses

---

## Configuration Properties Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `metabase.url` | String | *required* | Base URL of Metabase |
| `metabase.api-key` | String | *required* | API key for authentication |
| `metabase.connect-timeout` | Duration | 10s | Connection timeout |
| `metabase.read-timeout` | Duration | 30s | Read timeout |
| `metabase.max-connections` | int | 10 | Max connections in pool |

---

## API Authentication

Metabase supports two authentication methods:

### 1. API Key (Recommended for MCP Server)

```java
.defaultHeader("X-Metabase-Session", apiKey)
```

### 2. Session Token (Alternative)

Requires login endpoint call first, then use returned token.

---

## Verification Steps

```java
// Simple verification test
@SpringBootTest
class MetabaseClientConfigTest {

    @Autowired
    private MetabaseProperties properties;

    @Autowired
    private WebClient metabaseWebClient;

    @Autowired
    private ApiClient apiClient;

    @Test
    void propertiesLoaded() {
        assertNotNull(properties.getUrl());
        assertNotNull(properties.getApiKey());
    }

    @Test
    void webClientConfigured() {
        assertNotNull(metabaseWebClient);
    }

    @Test
    void apiClientConfigured() {
        assertNotNull(apiClient);
        assertEquals(properties.getUrl(), apiClient.getBasePath());
    }
}
```

---

## Environment Variables

For production deployments, use environment variables:

```bash
export METABASE_URL=http://metabase.example.com
export METABASE_API_KEY=mb_xxxxxxxxxxxxxxxxxxxx
```

And reference in properties:
```properties
metabase.url=${METABASE_URL}
metabase.api-key=${METABASE_API_KEY}
```

---

## Commit Template

```
feat(client): add Metabase client configuration

- Create MetabaseProperties for configuration binding
- Create MetabaseClientConfig with WebClient setup
- Add timeout and connection pool configuration
- Implement request/response logging filters
- Add MetabaseApiException for error handling
- Update application.properties with Metabase settings

Part of #2
```

---

## Notes

- API key should never be committed to version control
- Use environment variables for sensitive configuration
- Connection pool size should match expected concurrent usage
- Timeouts may need adjustment based on Metabase server performance
- Consider adding retry logic for transient failures
