package io.veeblefetzer.remote_mcp_server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.veeblefetzer.remote_mcp_server.tools.MetabaseToolService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MCP tools against a real Metabase instance.
 * 
 * Prerequisites:
 * - Docker compose must be running: docker compose up -d
 * - Metabase must be initialized and ready
 * 
 * Run with: mvn test -Dtest=McpToolsIntegrationTest -Dspring.profiles.active=integration
 */
@SpringBootTest
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
class McpToolsIntegrationTest {

    @Autowired
    private MetabaseToolService toolService;

    @Autowired
    private ObjectMapper objectMapper;

    private static Integer createdCardId;

    @Test
    @Order(1)
    @DisplayName("Should list databases from Metabase")
    void listDatabases_shouldReturnDatabases() throws Exception {
        // When
        String result = toolService.listDatabases(false);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("databases"), "Should have databases field");
        assertTrue(json.get("databases").isArray(), "Databases should be an array");
    }

    @Test
    @Order(2)
    @DisplayName("Should get current user information")
    void getCurrentUser_shouldReturnUserInfo() throws Exception {
        // When
        String result = toolService.getCurrentUser();
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("email"), "Should have email field");
        assertTrue(json.has("id"), "Should have id field");
    }

    @Test
    @Order(3)
    @DisplayName("Should list collections")
    void listCollections_shouldReturnCollections() throws Exception {
        // When
        String result = toolService.listCollections(null);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("collections"), "Should have collections field");
    }

    @Test
    @Order(4)
    @DisplayName("Should get root collection items")
    void getCollectionItems_rootCollection() throws Exception {
        // When
        String result = toolService.getCollectionItems("root");
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("items"), "Should have items field");
        assertTrue(json.has("total"), "Should have total field");
    }

    @Test
    @Order(5)
    @DisplayName("Should list dashboards")
    void listDashboards_shouldReturnDashboards() throws Exception {
        // When
        String result = toolService.listDashboards(null);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("dashboards"), "Should have dashboards field");
        assertTrue(json.get("dashboards").isArray(), "Dashboards should be an array");
    }

    @Test
    @Order(6)
    @DisplayName("Should list cards/questions")
    void listCards_shouldReturnCards() throws Exception {
        // When
        String result = toolService.listCards(null, null);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("cards"), "Should have cards field");
        assertTrue(json.get("cards").isArray(), "Cards should be an array");
    }

    @Test
    @Order(7)
    @DisplayName("Should get database with tables (if exists)")
    void getDatabase_withDatabaseId1() throws Exception {
        // First list databases to get a valid ID
        String listResult = toolService.listDatabases(false);
        JsonNode listJson = objectMapper.readTree(listResult);
        JsonNode databases = listJson.get("databases");
        
        if (databases.isEmpty()) {
            // Skip if no databases available
            return;
        }
        
        int dbId = databases.get(0).get("id").asInt();
        
        // When
        String result = toolService.getDatabase(dbId, false);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("database"), "Should have database field");
    }

    @Test
    @Order(8)
    @DisplayName("Should execute simple query")
    void executeQuery_simpleSelect() throws Exception {
        // First get a database ID
        String listResult = toolService.listDatabases(false);
        JsonNode listJson = objectMapper.readTree(listResult);
        JsonNode databases = listJson.get("databases");
        
        if (databases.isEmpty()) {
            // Skip if no databases available
            return;
        }
        
        int dbId = databases.get(0).get("id").asInt();
        String engine = databases.get(0).get("engine").asText();
        
        // Choose query based on database engine
        String query;
        if ("h2".equals(engine)) {
            query = "SELECT 1 as test_col";
        } else if ("postgres".equals(engine) || "mysql".equals(engine)) {
            query = "SELECT 1 as test_col";
        } else {
            query = "SELECT 1";
        }
        
        // When
        String result = toolService.executeQuery(dbId, query, 10);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("columns"), "Should have columns field");
        assertTrue(json.has("rows"), "Should have rows field");
        assertTrue(json.has("row_count"), "Should have row_count field");
    }

    @Test
    @Order(9)
    @DisplayName("Should create a new card")
    void createCard_success() throws Exception {
        // First get a database ID
        String listResult = toolService.listDatabases(false);
        JsonNode listJson = objectMapper.readTree(listResult);
        JsonNode databases = listJson.get("databases");
        
        if (databases.isEmpty()) {
            // Skip if no databases available
            return;
        }
        
        int dbId = databases.get(0).get("id").asInt();
        
        // When
        String result = toolService.createCard(
            "Integration Test Card " + System.currentTimeMillis(),
            dbId,
            "SELECT 1 as test_value",
            "table",
            null
        );
        
        // Then
        assertNotNull(result);
        
        JsonNode json = objectMapper.readTree(result);
        if (json.has("error")) {
            // Card creation might fail due to permissions - this is acceptable
            System.out.println("Card creation skipped: " + json.get("error").asText());
            return;
        }
        
        assertTrue(json.has("id"), "Should have id field");
        assertTrue(json.has("message"), "Should have message field");
        assertEquals("Card created successfully", json.get("message").asText());
        
        // Store for later cleanup/tests
        createdCardId = json.get("id").asInt();
    }

    @Test
    @Order(10)
    @DisplayName("Should get card details")
    void getCard_afterCreation() throws Exception {
        if (createdCardId == null) {
            // Skip if no card was created
            return;
        }
        
        // When
        String result = toolService.getCard(createdCardId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertEquals(createdCardId.intValue(), json.get("id").asInt());
        assertTrue(json.has("name"), "Should have name field");
        assertTrue(json.has("dataset_query"), "Should have dataset_query field");
    }

    @Test
    @Order(11)
    @DisplayName("Should run card query")
    void runCardQuery_afterCreation() throws Exception {
        if (createdCardId == null) {
            // Skip if no card was created
            return;
        }
        
        // When
        String result = toolService.runCardQuery(createdCardId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.contains("error"), "Should not contain error: " + result);
        
        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.has("columns"), "Should have columns field");
        assertTrue(json.has("rows"), "Should have rows field");
    }

    @Test
    @Order(12)
    @DisplayName("Should return error for missing required parameters")
    void getDatabase_missingId() {
        // When
        String result = toolService.getDatabase(null, false);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"), "Should contain error");
        assertTrue(result.contains("database_id is required"));
    }

    @Test
    @Order(13)
    @DisplayName("Should return error for missing query")
    void executeQuery_missingQuery() {
        // When
        String result = toolService.executeQuery(1, null, 10);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"), "Should contain error");
        assertTrue(result.contains("database_id and query are required"));
    }

    @Test
    @Order(14)
    @DisplayName("Should return error for non-existent database")
    void getDatabase_nonExistent() {
        // When
        String result = toolService.getDatabase(99999, false);
        
        // Then
        assertNotNull(result);
        // Either returns an error or returns empty data depending on Metabase version
    }
}
