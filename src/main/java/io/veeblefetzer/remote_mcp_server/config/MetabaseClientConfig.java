package io.veeblefetzer.remote_mcp_server.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.veeblefetzer.metabase.client.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for the Metabase API client.
 * Sets up WebClient with proper authentication, timeouts, and error handling.
 */
@Configuration
@EnableConfigurationProperties(MetabaseProperties.class)
public class MetabaseClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(MetabaseClientConfig.class);

    private final MetabaseProperties properties;

    public MetabaseClientConfig(MetabaseProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a configured WebClient for Metabase API calls.
     * Includes authentication headers, timeouts, and logging filters.
     *
     * @return configured WebClient instance
     */
    @Bean
    public WebClient metabaseWebClient() {
        // Configure connection provider with pool settings
        ConnectionProvider connectionProvider = ConnectionProvider.builder("metabase-pool")
                .maxConnections(properties.getMaxConnections())
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .build();

        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.getConnectTimeout().toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                properties.getReadTimeout().toMillis(),
                                TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Metabase-Session", properties.getApiKey())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleErrors())
                .build();
    }

    /**
     * Creates the generated Metabase API client configured with our WebClient.
     *
     * @param metabaseWebClient the configured WebClient
     * @return configured ApiClient instance
     */
    @Bean
    public ApiClient metabaseApiClient(WebClient metabaseWebClient) {
        ApiClient apiClient = new ApiClient(metabaseWebClient);
        apiClient.setBasePath(properties.getUrl());
        return apiClient;
    }

    /**
     * Filter to log outgoing requests.
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Metabase API Request: {} {}", request.method(), request.url());
                request.headers().forEach((name, values) -> {
                    // Don't log sensitive headers
                    if (!name.equalsIgnoreCase("X-Metabase-Session")) {
                        values.forEach(value -> logger.debug("  {}: {}", name, value));
                    } else {
                        logger.debug("  {}: [REDACTED]", name);
                    }
                });
            }
            return Mono.just(request);
        });
    }

    /**
     * Filter to log incoming responses.
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Metabase API Response: {} {}",
                        response.statusCode().value(),
                        response.statusCode());
            }
            return Mono.just(response);
        });
    }

    /**
     * Filter to handle error responses and convert them to MetabaseApiException.
     */
    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            logger.error("Metabase API error: {} - {}",
                                    response.statusCode().value(), body);
                            return Mono.error(new MetabaseApiException(
                                    response.statusCode().value(),
                                    body));
                        });
            }
            return Mono.just(response);
        });
    }
}
