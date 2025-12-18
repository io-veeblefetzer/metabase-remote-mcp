package io.veeblefetzer.remote_mcp_server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetabaseResourceService.
 */
@ExtendWith(MockitoExtension.class)
class MetabaseResourceServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper;
    private MetabaseResourceService resourceService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resourceService = new MetabaseResourceService(webClient, objectMapper);
    }

    @Test
    void listResourceTemplates_returnsAllTemplates() {
        // When
        List<Map<String, Object>> templates = resourceService.listResourceTemplates();

        // Then
        assertNotNull(templates);
        assertEquals(5, templates.size());
        
        // Verify template structure
        assertTrue(templates.stream().anyMatch(t -> 
                t.get("uriTemplate").equals("metabase://database/{id}")));
        assertTrue(templates.stream().anyMatch(t -> 
                t.get("uriTemplate").equals("metabase://database/{id}/tables")));
        assertTrue(templates.stream().anyMatch(t -> 
                t.get("uriTemplate").equals("metabase://dashboard/{id}")));
        assertTrue(templates.stream().anyMatch(t -> 
                t.get("uriTemplate").equals("metabase://card/{id}")));
        assertTrue(templates.stream().anyMatch(t -> 
                t.get("uriTemplate").equals("metabase://collection/{id}")));
    }

    @Test
    void listResourceTemplates_hasCorrectProperties() {
        // When
        List<Map<String, Object>> templates = resourceService.listResourceTemplates();

        // Then
        for (Map<String, Object> template : templates) {
            assertTrue(template.containsKey("uriTemplate"), "Should have uriTemplate");
            assertTrue(template.containsKey("name"), "Should have name");
            assertTrue(template.containsKey("description"), "Should have description");
            assertTrue(template.containsKey("mimeType"), "Should have mimeType");
            assertEquals("application/json", template.get("mimeType"));
        }
    }

    @Test
    void resolveResource_unknownUri_returnsError() {
        // When
        String result = resourceService.resolveResource("unknown://uri");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Unknown resource URI"));
    }

    @Test
    void resolveResource_invalidFormat_returnsError() {
        // When
        String result = resourceService.resolveResource("metabase://invalid/123");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
    }

    @Test
    void resolveDatabase_success() throws Exception {
        // Given
        String jsonResponse = """
            {
                "id": 1, 
                "name": "Test DB", 
                "engine": "postgres",
                "description": "A test database",
                "is_sample": true,
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-02T00:00:00Z"
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/database/1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = resourceService.resolveResource("metabase://database/1");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Test DB"));
        assertTrue(result.contains("postgres"));
    }

    @Test
    void resolveDatabaseTables_success() throws Exception {
        // Given
        String jsonResponse = """
            {
                "tables": [
                    {
                        "id": 1, 
                        "name": "users", 
                        "schema": "public",
                        "display_name": "Users",
                        "description": "User table",
                        "fields": [
                            {"id": 1, "name": "id", "display_name": "ID", "base_type": "type/Integer", "semantic_type": "type/PK"},
                            {"id": 2, "name": "name", "display_name": "Name", "base_type": "type/Text", "semantic_type": null}
                        ]
                    }
                ]
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/database/1/metadata")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = resourceService.resolveResource("metabase://database/1/tables");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("users"));
        assertTrue(result.contains("tables"));
        assertTrue(result.contains("fields"));
    }

    @Test
    void resolveDashboard_success() throws Exception {
        // Given
        String jsonResponse = """
            {
                "id": 1, 
                "name": "Sales Dashboard", 
                "description": "Sales metrics",
                "collection_id": 5,
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-02T00:00:00Z",
                "dashcards": [
                    {"id": 1, "card_id": 10, "row": 0, "col": 0, "size_x": 6, "size_y": 4}
                ],
                "parameters": []
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/dashboard/1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = resourceService.resolveResource("metabase://dashboard/1");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Sales Dashboard"));
        assertTrue(result.contains("dashcards"));
    }

    @Test
    void resolveCard_success() throws Exception {
        // Given
        String jsonResponse = """
            {
                "id": 1, 
                "name": "Revenue Query", 
                "description": "Total revenue by month",
                "database_id": 1,
                "collection_id": 5,
                "query_type": "native",
                "display": "bar",
                "dataset_query": {"database": 1, "type": "native", "native": {"query": "SELECT 1"}},
                "visualization_settings": {},
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-02T00:00:00Z"
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/card/1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = resourceService.resolveResource("metabase://card/1");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Revenue Query"));
        assertTrue(result.contains("dataset_query"));
    }

    @Test
    void resolveCollection_success() throws Exception {
        // Given
        String jsonResponse = """
            {
                "data": [
                    {"id": 1, "name": "Dashboard 1", "model": "dashboard", "description": "First dashboard"},
                    {"id": 2, "name": "Query 1", "model": "card", "description": "First query"}
                ],
                "total": 2
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/collection/5/items")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = resourceService.resolveResource("metabase://collection/5");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Dashboard 1"));
        assertTrue(result.contains("Query 1"));
        assertTrue(result.contains("items"));
        assertTrue(result.contains("total"));
    }

    @Test
    void resolveCollection_rootCollection() throws Exception {
        // Given
        String jsonResponse = """
            {
                "data": [
                    {"id": 1, "name": "Root Item", "model": "card", "description": "An item in root"}
                ],
                "total": 1
            }
            """;
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/collection/root/items")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(responseNode));

        // When
        String result = resourceService.resolveResource("metabase://collection/root");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Root Item"));
    }

    @Test
    void resolveDatabase_notFound() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/database/999")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.empty());

        // When
        String result = resourceService.resolveResource("metabase://database/999");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("not found"));
    }
}
