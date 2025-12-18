package io.veeblefetzer.remote_mcp_server.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetabaseToolService.
 */
@ExtendWith(MockitoExtension.class)
class MetabaseToolServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper;
    private MetabaseToolService toolService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        toolService = new MetabaseToolService(webClient, objectMapper);
    }

    @Test
    void getToolCallbacks_returnsNonEmptyArray() {
        var callbacks = toolService.getToolCallbacks();
        assertNotNull(callbacks);
        assertTrue(callbacks.length > 0, "Should have at least one tool callback");
    }

    @Test
    void listDatabases_success() throws Exception {
        // Given
        String jsonResponse = """
            {
                "data": [
                    {"id": 1, "name": "Sample DB", "engine": "postgres", "is_sample": true},
                    {"id": 2, "name": "Production DB", "engine": "mysql", "is_sample": false}
                ]
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = toolService.listDatabases(false);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Sample DB"));
        assertTrue(result.contains("databases"));
    }

    @Test
    void listDatabases_noResponse() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.empty());

        // When
        String result = toolService.listDatabases(false);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("No response from Metabase"));
    }

    @Test
    void getDatabase_missingId_returnsError() {
        // When
        String result = toolService.getDatabase(null, false);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("database_id is required"));
    }

    @Test
    void getDatabase_success() throws Exception {
        // Given
        String jsonResponse = """
            {"id": 1, "name": "Test DB", "engine": "postgres"}
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/database/1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = toolService.getDatabase(1, false);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Test DB"));
        assertTrue(result.contains("database"));
    }

    @Test
    void executeQuery_missingDatabaseId_returnsError() {
        // When
        String result = toolService.executeQuery(null, "SELECT 1", 100);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("database_id and query are required"));
    }

    @Test
    void executeQuery_missingQuery_returnsError() {
        // When
        String result = toolService.executeQuery(1, null, 100);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("database_id and query are required"));
    }

    @Test
    void getDashboard_missingId_returnsError() {
        // When
        String result = toolService.getDashboard(null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("dashboard_id is required"));
    }

    @Test
    void getCard_missingId_returnsError() {
        // When
        String result = toolService.getCard(null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("card_id is required"));
    }

    @Test
    void createCard_missingName_returnsError() {
        // When
        String result = toolService.createCard(null, 1, "SELECT 1", null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("name, database_id, and query are required"));
    }

    @Test
    void createCard_missingDatabaseId_returnsError() {
        // When
        String result = toolService.createCard("Test", null, "SELECT 1", null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("name, database_id, and query are required"));
    }

    @Test
    void createCard_missingQuery_returnsError() {
        // When
        String result = toolService.createCard("Test", 1, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("name, database_id, and query are required"));
    }

    @Test
    void runCardQuery_missingId_returnsError() {
        // When
        String result = toolService.runCardQuery(null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("card_id is required"));
    }

    @Test
    void getCollectionItems_missingId_returnsError() {
        // When
        String result = toolService.getCollectionItems(null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("collection_id is required"));
    }

    @Test
    void getTableMetadata_missingId_returnsError() {
        // When
        String result = toolService.getTableMetadata(null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("table_id is required"));
    }

    @Test
    void listDashboards_success() throws Exception {
        // Given
        String jsonResponse = """
            [
                {"id": 1, "name": "Sales Dashboard", "description": "Sales metrics", "collection_id": 1},
                {"id": 2, "name": "KPIs", "description": null, "collection_id": null}
            ]
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/dashboard")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = toolService.listDashboards(null);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Sales Dashboard"));
        assertTrue(result.contains("dashboards"));
    }

    @Test
    void listDashboards_filterByCollection() throws Exception {
        // Given
        String jsonResponse = """
            [
                {"id": 1, "name": "Sales Dashboard", "description": "Sales metrics", "collection_id": 1},
                {"id": 2, "name": "KPIs", "description": null, "collection_id": 2}
            ]
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/dashboard")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = toolService.listDashboards(1);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Sales Dashboard"));
        assertFalse(result.contains("KPIs"));
    }

    @Test
    void getCurrentUser_success() throws Exception {
        // Given
        String jsonResponse = """
            {
                "id": 1, 
                "email": "admin@example.com", 
                "first_name": "Admin", 
                "last_name": "User", 
                "is_superuser": true
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/user/current")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = toolService.getCurrentUser();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("admin@example.com"));
        assertTrue(result.contains("Admin"));
    }
}
