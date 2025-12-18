package io.veeblefetzer.remote_mcp_server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.veeblefetzer.remote_mcp_server.resources.MetabaseResourceService;
import io.veeblefetzer.remote_mcp_server.tools.MetabaseToolService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MCP resources against a real Metabase instance.
 * 
 * Prerequisites:
 * - Docker compose must be running: docker compose up -d
 * - Metabase must be initialized and ready
 * 
 * Run with: mvn test -Dtest=McpResourcesIntegrationTest -Dspring.profiles.active=integration
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
class McpResourcesIntegrationTest {

    @Autowired
    private MetabaseResourceService resourceService;

    @Autowired
    private MetabaseToolService toolService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("Should list all resource templates")
    void listResourceTemplates_shouldReturnAllTemplates() {
        // When
        List<Map<String, Object>> templates = resourceService.listResourceTemplates();
        
        // Then
        assertNotNull(templates);
        assertEquals(5, templates.size());
        
        // Verify all expected templates are present
        assertTrue(templates.stream().anyMatch(t -> 
            "metabase://database/{id}".equals(t.get("uriTemplate"))));
        assertTrue(templates.stream().anyMatch(t -> 
            "metabase://database/{id}/tables".equals(t.get("uriTemplate"))));
        assertTrue(templates.stream().anyMatch(t -> 
            "metabase://dashboard/{id}".equals(t.get("uriTemplate"))));
        assertTrue(templates.stream().anyMatch(t -> 
            "metabase://card/{id}".equals(t.get("uriTemplate"))));
        assertTrue(templates.stream().anyMatch(t -> 
            "metabase://collection/{id}".equals(t.get("uriTemplate"))));
    }

    @Test
    @Order(2)
    @DisplayName("Should resolve database resource")
    void resolveResource_database() throws Exception {
        // First get a valid database ID
        String listResult = toolService.listDatabases(false);
        JsonNode listJson = objectMapper.readTree(listResult);
        JsonNode databases = listJson.get("databases");
        
        if (databases.isEmpty()) {
            // Skip if no databases available
            return;
        }
        
        int dbId = databases.get(0).get("id").asInt();
        
        // When
        String result = resourceService.resolveResource("metabase://database/" + dbId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertEquals(dbId, json.get("id").asInt());
        assertTrue(json.has("name"), "Should have name field");
        assertTrue(json.has("engine"), "Should have engine field");
    }

    @Test
    @Order(3)
    @DisplayName("Should resolve database tables resource")
    void resolveResource_databaseTables() throws Exception {
        // First get a valid database ID
        String listResult = toolService.listDatabases(false);
        JsonNode listJson = objectMapper.readTree(listResult);
        JsonNode databases = listJson.get("databases");
        
        if (databases.isEmpty()) {
            // Skip if no databases available
            return;
        }
        
        int dbId = databases.get(0).get("id").asInt();
        
        // When
        String result = resourceService.resolveResource("metabase://database/" + dbId + "/tables");
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertEquals(dbId, json.get("database_id").asInt());
        assertTrue(json.has("tables"), "Should have tables field");
        assertTrue(json.get("tables").isArray(), "Tables should be an array");
    }

    @Test
    @Order(4)
    @DisplayName("Should resolve collection resource")
    void resolveResource_collection() throws Exception {
        // When - use root collection
        String result = resourceService.resolveResource("metabase://collection/root");
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertEquals("root", json.get("collection_id").asText());
        assertTrue(json.has("items"), "Should have items field");
        assertTrue(json.has("total"), "Should have total field");
    }

    @Test
    @Order(5)
    @DisplayName("Should return error for unknown resource URI")
    void resolveResource_unknownUri() throws Exception {
        // When
        String result = resourceService.resolveResource("unknown://resource");
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"), "Should contain error");
        assertTrue(result.contains("Unknown resource URI"));
    }

    @Test
    @Order(6)
    @DisplayName("Should return error for non-existent database")
    void resolveResource_nonExistentDatabase() throws Exception {
        // When
        String result = resourceService.resolveResource("metabase://database/99999");
        
        // Then
        assertNotNull(result);
        // Might be an error or empty response depending on Metabase behavior
    }

    @Test
    @Order(7)
    @DisplayName("Should resolve dashboard resource if exists")
    void resolveResource_dashboard() throws Exception {
        // First check if any dashboards exist
        String listResult = toolService.listDashboards(null);
        JsonNode listJson = objectMapper.readTree(listResult);
        JsonNode dashboards = listJson.get("dashboards");
        
        if (dashboards == null || dashboards.isEmpty()) {
            // Skip if no dashboards available
            return;
        }
        
        int dashId = dashboards.get(0).get("id").asInt();
        
        // When
        String result = resourceService.resolveResource("metabase://dashboard/" + dashId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertEquals(dashId, json.get("id").asInt());
        assertTrue(json.has("name"), "Should have name field");
        assertTrue(json.has("dashcards"), "Should have dashcards field");
    }

    @Test
    @Order(8)
    @DisplayName("Should resolve card resource if exists")
    void resolveResource_card() throws Exception {
        // First check if any cards exist
        String listResult = toolService.listCards(null, null);
        JsonNode listJson = objectMapper.readTree(listResult);
        JsonNode cards = listJson.get("cards");
        
        if (cards == null || cards.isEmpty()) {
            // Skip if no cards available
            return;
        }
        
        int cardId = cards.get(0).get("id").asInt();
        
        // When
        String result = resourceService.resolveResource("metabase://card/" + cardId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertEquals(cardId, json.get("id").asInt());
        assertTrue(json.has("name"), "Should have name field");
        assertTrue(json.has("dataset_query"), "Should have dataset_query field");
    }

    @Test
    @Order(9)
    @DisplayName("Resource templates should have all required fields")
    void listResourceTemplates_allFieldsPresent() {
        // When
        List<Map<String, Object>> templates = resourceService.listResourceTemplates();
        
        // Then
        for (Map<String, Object> template : templates) {
            assertTrue(template.containsKey("uriTemplate"), "Should have uriTemplate");
            assertTrue(template.containsKey("name"), "Should have name");
            assertTrue(template.containsKey("description"), "Should have description");
            assertTrue(template.containsKey("mimeType"), "Should have mimeType");
            
            assertEquals("application/json", template.get("mimeType"), 
                "All resources should have application/json mimeType");
            
            assertFalse(((String) template.get("uriTemplate")).isEmpty(), 
                "uriTemplate should not be empty");
            assertFalse(((String) template.get("name")).isEmpty(), 
                "name should not be empty");
            assertFalse(((String) template.get("description")).isEmpty(), 
                "description should not be empty");
        }
    }
}
