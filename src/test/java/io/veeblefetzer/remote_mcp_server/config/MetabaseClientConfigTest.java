package io.veeblefetzer.remote_mcp_server.config;

import io.veeblefetzer.metabase.client.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetabaseClientConfig.
 * Tests that the configuration loads correctly and beans are created.
 */
@SpringBootTest
@ActiveProfiles("test")
class MetabaseClientConfigTest {

    @Autowired
    private MetabaseProperties properties;

    @Autowired
    private WebClient metabaseWebClient;

    @Autowired
    private ApiClient apiClient;

    @Test
    void contextLoads() {
        // Verifies Spring context loads with our configuration
    }

    @Test
    void propertiesAreLoaded() {
        assertNotNull(properties);
        assertNotNull(properties.getUrl());
        assertFalse(properties.getUrl().isBlank());
        assertEquals("http://localhost:3000", properties.getUrl());
    }

    @Test
    void apiKeyIsLoaded() {
        assertNotNull(properties.getApiKey());
        assertFalse(properties.getApiKey().isBlank());
    }

    @Test
    void webClientBeanIsConfigured() {
        assertNotNull(metabaseWebClient);
    }

    @Test
    void apiClientBeanIsConfigured() {
        assertNotNull(apiClient);
        assertEquals(properties.getUrl(), apiClient.getBasePath());
    }

    @Test
    void timeoutPropertiesHaveDefaults() {
        assertNotNull(properties.getConnectTimeout());
        assertNotNull(properties.getReadTimeout());
        assertTrue(properties.getConnectTimeout().toMillis() > 0);
        assertTrue(properties.getReadTimeout().toMillis() > 0);
    }

    @Test
    void maxConnectionsHasDefault() {
        assertTrue(properties.getMaxConnections() > 0);
    }
}
