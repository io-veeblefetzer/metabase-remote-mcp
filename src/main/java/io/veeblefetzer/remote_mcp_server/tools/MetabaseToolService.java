package io.veeblefetzer.remote_mcp_server.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Service providing all Metabase MCP tools.
 * Each public method annotated with @Tool becomes an MCP tool.
 */
@Service
@Primary
public class MetabaseToolService implements ToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(MetabaseToolService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ToolCallbackProvider delegateProvider;

    public MetabaseToolService(WebClient metabaseWebClient, ObjectMapper objectMapper) {
        this.webClient = metabaseWebClient;
        this.objectMapper = objectMapper;

        // Create delegate provider from this service's methods
        this.delegateProvider = MethodToolCallbackProvider.builder()
                .toolObjects(this)
                .build();
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return delegateProvider.getToolCallbacks();
    }

    // ==================== Database Tools ====================

    @Tool(name = "list-databases", description = "List all databases connected to Metabase. Returns database IDs, names, and connection details. Use this to discover available data sources.")
    public String listDatabases(
            @ToolParam(description = "Include table information in response", required = false) Boolean includeTables
    ) {
        log.info("Executing list-databases tool, includeTables={}", includeTables);
        try {
            String include = (includeTables != null && includeTables) ? "tables" : null;
            
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/database");
                        if (include != null) {
                            uriBuilder.queryParam("include", include);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No response from Metabase");
            }

            // Extract and simplify the database list
            List<Map<String, Object>> databases = new ArrayList<>();
            JsonNode dataNode = response.has("data") ? response.get("data") : response;
            
            if (dataNode.isArray()) {
                for (JsonNode db : dataNode) {
                    Map<String, Object> dbInfo = new LinkedHashMap<>();
                    dbInfo.put("id", db.path("id").asInt());
                    dbInfo.put("name", db.path("name").asText());
                    dbInfo.put("engine", db.path("engine").asText());
                    if (db.has("is_sample")) {
                        dbInfo.put("is_sample", db.path("is_sample").asBoolean());
                    }
                    databases.add(dbInfo);
                }
            }

            return successResponse(Map.of("databases", databases));
        } catch (Exception e) {
            log.error("Failed to list databases", e);
            return errorResponse("Failed to list databases: " + e.getMessage());
        }
    }

    @Tool(name = "get-database", description = "Get detailed information about a specific database, including connection settings and available tables.")
    public String getDatabase(
            @ToolParam(description = "The ID of the database to retrieve") Integer databaseId,
            @ToolParam(description = "Include table and field metadata", required = false) Boolean includeMetadata
    ) {
        log.info("Executing get-database tool, databaseId={}, includeMetadata={}", databaseId, includeMetadata);
        if (databaseId == null) {
            return errorResponse("database_id is required");
        }

        try {
            String path = (includeMetadata != null && includeMetadata) 
                    ? "/api/database/" + databaseId + "/metadata"
                    : "/api/database/" + databaseId;
            
            JsonNode response = webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Database not found");
            }

            return successResponse(Map.of("database", nodeToMap(response)));
        } catch (Exception e) {
            log.error("Failed to get database {}", databaseId, e);
            return errorResponse("Failed to get database: " + e.getMessage());
        }
    }

    // ==================== Query Tools ====================

    @Tool(name = "execute-query", description = "Execute a native SQL query against a database. Returns query results as rows and columns. Use with caution - queries are executed directly.")
    public String executeQuery(
            @ToolParam(description = "The database ID to query") Integer databaseId,
            @ToolParam(description = "The SQL query to execute") String query,
            @ToolParam(description = "Maximum number of rows to return (default: 100)", required = false) Integer limit
    ) {
        log.info("Executing execute-query tool, databaseId={}, limit={}", databaseId, limit);
        if (databaseId == null || query == null) {
            return errorResponse("database_id and query are required");
        }

        int effectiveLimit = (limit != null && limit > 0) ? limit : 100;

        // Add LIMIT clause if not already present
        String effectiveQuery = query.trim();
        if (!effectiveQuery.toLowerCase().contains("limit")) {
            effectiveQuery = effectiveQuery.replaceAll(";\\s*$", "") + " LIMIT " + effectiveLimit;
        }

        try {
            // Build the query request
            Map<String, Object> nativeQuery = new LinkedHashMap<>();
            nativeQuery.put("query", effectiveQuery);

            Map<String, Object> queryRequest = new LinkedHashMap<>();
            queryRequest.put("database", databaseId);
            queryRequest.put("type", "native");
            queryRequest.put("native", nativeQuery);

            JsonNode response = webClient.post()
                    .uri("/api/dataset")
                    .bodyValue(queryRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No response from query execution");
            }

            // Extract columns and rows from response
            Map<String, Object> result = new LinkedHashMap<>();
            List<String> columns = new ArrayList<>();
            List<List<Object>> rows = new ArrayList<>();

            if (response.has("data")) {
                JsonNode dataNode = response.get("data");
                
                // Extract column names
                if (dataNode.has("cols")) {
                    for (JsonNode col : dataNode.get("cols")) {
                        columns.add(col.has("name") ? col.get("name").asText() : "unknown");
                    }
                }
                
                // Extract rows
                if (dataNode.has("rows")) {
                    for (JsonNode row : dataNode.get("rows")) {
                        List<Object> rowData = new ArrayList<>();
                        for (JsonNode cell : row) {
                            rowData.add(nodeToValue(cell));
                        }
                        rows.add(rowData);
                    }
                }
            }

            result.put("columns", columns);
            result.put("rows", rows);
            result.put("row_count", rows.size());

            return successResponse(result);
        } catch (Exception e) {
            log.error("Query execution failed", e);
            return errorResponse("Query failed: " + e.getMessage());
        }
    }

    // ==================== Dashboard Tools ====================

    @Tool(name = "list-dashboards", description = "List all dashboards in Metabase. Returns dashboard IDs, names, and descriptions. Use to discover available dashboards.")
    public String listDashboards(
            @ToolParam(description = "Filter by collection ID (optional)", required = false) Integer collectionId
    ) {
        log.info("Executing list-dashboards tool, collectionId={}", collectionId);
        try {
            JsonNode response = webClient.get()
                    .uri("/api/dashboard")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No response from Metabase");
            }

            List<Map<String, Object>> dashboards = new ArrayList<>();
            for (JsonNode dashboard : response) {
                // Filter by collection if specified
                Integer dashCollectionId = dashboard.has("collection_id") && !dashboard.get("collection_id").isNull() 
                        ? dashboard.get("collection_id").asInt() : null;
                if (collectionId != null && !collectionId.equals(dashCollectionId)) {
                    continue;
                }

                Map<String, Object> dashInfo = new LinkedHashMap<>();
                dashInfo.put("id", dashboard.path("id").asInt());
                dashInfo.put("name", dashboard.path("name").asText());
                dashInfo.put("description", dashboard.path("description").asText(""));
                dashInfo.put("collection_id", dashCollectionId);
                dashboards.add(dashInfo);
            }

            return successResponse(Map.of("dashboards", dashboards));
        } catch (Exception e) {
            log.error("Failed to list dashboards", e);
            return errorResponse("Failed to list dashboards: " + e.getMessage());
        }
    }

    @Tool(name = "get-dashboard", description = "Get detailed information about a dashboard, including all cards (questions) and their configurations.")
    public String getDashboard(
            @ToolParam(description = "The ID of the dashboard to retrieve") Integer dashboardId
    ) {
        log.info("Executing get-dashboard tool, dashboardId={}", dashboardId);
        if (dashboardId == null) {
            return errorResponse("dashboard_id is required");
        }

        try {
            JsonNode response = webClient.get()
                    .uri("/api/dashboard/" + dashboardId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Dashboard not found");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("name", response.path("name").asText());
            result.put("description", response.path("description").asText(""));
            result.put("collection_id", response.has("collection_id") && !response.get("collection_id").isNull() 
                    ? response.get("collection_id").asInt() : null);

            // Extract cards from dashboard
            List<Map<String, Object>> cards = new ArrayList<>();
            JsonNode dashcards = response.path("dashcards");
            if (dashcards.isArray()) {
                for (JsonNode dashcard : dashcards) {
                    Map<String, Object> cardInfo = new LinkedHashMap<>();
                    cardInfo.put("id", dashcard.path("id").asInt());
                    cardInfo.put("card_id", dashcard.has("card_id") && !dashcard.get("card_id").isNull() 
                            ? dashcard.get("card_id").asInt() : null);
                    cardInfo.put("size_x", dashcard.path("size_x").asInt());
                    cardInfo.put("size_y", dashcard.path("size_y").asInt());
                    cardInfo.put("row", dashcard.path("row").asInt());
                    cardInfo.put("col", dashcard.path("col").asInt());
                    cards.add(cardInfo);
                }
            }
            result.put("cards", cards);

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to get dashboard {}", dashboardId, e);
            return errorResponse("Failed to get dashboard: " + e.getMessage());
        }
    }

    // ==================== Card/Question Tools ====================

    @Tool(name = "list-cards", description = "List all saved questions (cards) in Metabase. Returns card IDs, names, and query types.")
    public String listCards(
            @ToolParam(description = "Filter by collection ID (optional)", required = false) Integer collectionId,
            @ToolParam(description = "Filter by database ID (optional)", required = false) Integer databaseId
    ) {
        log.info("Executing list-cards tool, collectionId={}, databaseId={}", collectionId, databaseId);
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/card")
                            .queryParam("f", "all")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No response from Metabase");
            }

            List<Map<String, Object>> cards = new ArrayList<>();
            for (JsonNode card : response) {
                // Filter by collection if specified
                Integer cardCollectionId = card.has("collection_id") && !card.get("collection_id").isNull() 
                        ? card.get("collection_id").asInt() : null;
                if (collectionId != null && !collectionId.equals(cardCollectionId)) {
                    continue;
                }
                // Filter by database if specified
                Integer cardDatabaseId = card.has("database_id") && !card.get("database_id").isNull() 
                        ? card.get("database_id").asInt() : null;
                if (databaseId != null && !databaseId.equals(cardDatabaseId)) {
                    continue;
                }

                Map<String, Object> cardInfo = new LinkedHashMap<>();
                cardInfo.put("id", card.path("id").asInt());
                cardInfo.put("name", card.path("name").asText());
                cardInfo.put("description", card.path("description").asText(""));
                cardInfo.put("database_id", cardDatabaseId);
                cardInfo.put("collection_id", cardCollectionId);
                cardInfo.put("display", card.path("display").asText());
                cards.add(cardInfo);
            }

            return successResponse(Map.of("cards", cards));
        } catch (Exception e) {
            log.error("Failed to list cards", e);
            return errorResponse("Failed to list cards: " + e.getMessage());
        }
    }

    @Tool(name = "get-card", description = "Get detailed information about a saved question (card), including the query definition and visualization settings.")
    public String getCard(
            @ToolParam(description = "The ID of the card to retrieve") Integer cardId
    ) {
        log.info("Executing get-card tool, cardId={}", cardId);
        if (cardId == null) {
            return errorResponse("card_id is required");
        }

        try {
            JsonNode response = webClient.get()
                    .uri("/api/card/" + cardId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Card not found");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("name", response.path("name").asText());
            result.put("description", response.path("description").asText(""));
            result.put("database_id", response.has("database_id") && !response.get("database_id").isNull() 
                    ? response.get("database_id").asInt() : null);
            result.put("collection_id", response.has("collection_id") && !response.get("collection_id").isNull() 
                    ? response.get("collection_id").asInt() : null);
            result.put("display", response.path("display").asText());
            result.put("query_type", response.path("query_type").asText());
            result.put("dataset_query", nodeToMap(response.path("dataset_query")));
            result.put("visualization_settings", nodeToMap(response.path("visualization_settings")));

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to get card {}", cardId, e);
            return errorResponse("Failed to get card: " + e.getMessage());
        }
    }

    @Tool(name = "create-card", description = "Create a new saved question (card) in Metabase. Requires query definition and visualization settings.")
    public String createCard(
            @ToolParam(description = "Name of the question") String name,
            @ToolParam(description = "Database to query") Integer databaseId,
            @ToolParam(description = "SQL query to execute") String query,
            @ToolParam(description = "Type of visualization (table, bar, line, pie, etc.)", required = false) String visualizationType,
            @ToolParam(description = "Collection to save the card in (optional)", required = false) Integer collectionId
    ) {
        log.info("Executing create-card tool, name={}, databaseId={}", name, databaseId);
        if (name == null || databaseId == null || query == null) {
            return errorResponse("name, database_id, and query are required");
        }

        String display = visualizationType != null ? visualizationType : "table";

        try {
            // Build the card creation request
            Map<String, Object> nativeQuery = new LinkedHashMap<>();
            nativeQuery.put("query", query);

            Map<String, Object> datasetQuery = new LinkedHashMap<>();
            datasetQuery.put("database", databaseId);
            datasetQuery.put("type", "native");
            datasetQuery.put("native", nativeQuery);

            Map<String, Object> cardRequest = new LinkedHashMap<>();
            cardRequest.put("name", name);
            cardRequest.put("dataset_query", datasetQuery);
            cardRequest.put("display", display);
            cardRequest.put("visualization_settings", Map.of());
            if (collectionId != null) {
                cardRequest.put("collection_id", collectionId);
            }

            JsonNode response = webClient.post()
                    .uri("/api/card")
                    .bodyValue(cardRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Failed to create card");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("name", response.path("name").asText());
            result.put("message", "Card created successfully");

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to create card", e);
            return errorResponse("Failed to create card: " + e.getMessage());
        }
    }

    @Tool(name = "run-card-query", description = "Execute a saved question (card) and return its results. Use this to get data from an existing saved question.")
    public String runCardQuery(
            @ToolParam(description = "The ID of the card to execute") Integer cardId
    ) {
        log.info("Executing run-card-query tool, cardId={}", cardId);
        if (cardId == null) {
            return errorResponse("card_id is required");
        }

        try {
            JsonNode response = webClient.post()
                    .uri("/api/card/" + cardId + "/query")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No response from query execution");
            }

            // Extract columns and rows
            Map<String, Object> result = new LinkedHashMap<>();
            List<String> columns = new ArrayList<>();
            List<List<Object>> rows = new ArrayList<>();

            if (response.has("data")) {
                JsonNode dataNode = response.get("data");
                
                if (dataNode.has("cols")) {
                    for (JsonNode col : dataNode.get("cols")) {
                        columns.add(col.has("name") ? col.get("name").asText() : "unknown");
                    }
                }
                
                if (dataNode.has("rows")) {
                    for (JsonNode row : dataNode.get("rows")) {
                        List<Object> rowData = new ArrayList<>();
                        for (JsonNode cell : row) {
                            rowData.add(nodeToValue(cell));
                        }
                        rows.add(rowData);
                    }
                }
            }

            result.put("columns", columns);
            result.put("rows", rows);
            result.put("row_count", rows.size());

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to run card query {}", cardId, e);
            return errorResponse("Failed to run card query: " + e.getMessage());
        }
    }

    // ==================== User Tools ====================

    @Tool(name = "get-current-user", description = "Get information about the currently authenticated user, including name, email, and permissions.")
    public String getCurrentUser() {
        log.info("Executing get-current-user tool");
        try {
            JsonNode response = webClient.get()
                    .uri("/api/user/current")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No user information available");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("email", response.path("email").asText());
            result.put("first_name", response.path("first_name").asText());
            result.put("last_name", response.path("last_name").asText());
            result.put("is_superuser", response.path("is_superuser").asBoolean());

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return errorResponse("Failed to get current user: " + e.getMessage());
        }
    }

    // ==================== Collection Tools ====================

    @Tool(name = "list-collections", description = "List all collections in Metabase. Collections organize dashboards and questions into folders.")
    public String listCollections(
            @ToolParam(description = "Parent collection ID to filter by (optional)", required = false) Integer parentId
    ) {
        log.info("Executing list-collections tool, parentId={}", parentId);
        try {
            JsonNode response = webClient.get()
                    .uri("/api/collection")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No response from Metabase");
            }

            List<Map<String, Object>> collections = new ArrayList<>();
            for (JsonNode collection : response) {
                // Filter by parent if specified
                Object collectionParentId = collection.has("parent_id") && !collection.get("parent_id").isNull() 
                        ? collection.get("parent_id").asInt() : null;
                if (parentId != null) {
                    if (collectionParentId == null || !parentId.equals(collectionParentId)) {
                        continue;
                    }
                }

                Map<String, Object> collInfo = new LinkedHashMap<>();
                collInfo.put("id", collection.path("id").asText()); // collection IDs can be strings like "root"
                collInfo.put("name", collection.path("name").asText());
                collInfo.put("description", collection.path("description").asText(""));
                collInfo.put("parent_id", collectionParentId);
                collections.add(collInfo);
            }

            return successResponse(Map.of("collections", collections));
        } catch (Exception e) {
            log.error("Failed to list collections", e);
            return errorResponse("Failed to list collections: " + e.getMessage());
        }
    }

    @Tool(name = "get-collection-items", description = "Get all items in a collection, including dashboards, questions, and sub-collections.")
    public String getCollectionItems(
            @ToolParam(description = "The ID of the collection (use 'root' for root collection)") String collectionId
    ) {
        log.info("Executing get-collection-items tool, collectionId={}", collectionId);
        if (collectionId == null) {
            return errorResponse("collection_id is required");
        }

        try {
            JsonNode response = webClient.get()
                    .uri("/api/collection/" + collectionId + "/items")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("No response from Metabase");
            }

            List<Map<String, Object>> items = new ArrayList<>();
            JsonNode dataNode = response.has("data") ? response.get("data") : response;
            if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    Map<String, Object> itemInfo = new LinkedHashMap<>();
                    itemInfo.put("id", item.path("id").asInt());
                    itemInfo.put("name", item.path("name").asText());
                    itemInfo.put("model", item.path("model").asText());
                    itemInfo.put("description", item.path("description").asText(""));
                    items.add(itemInfo);
                }
            }

            Integer total = response.has("total") ? response.get("total").asInt() : items.size();
            return successResponse(Map.of("items", items, "total", total));
        } catch (Exception e) {
            log.error("Failed to get collection items {}", collectionId, e);
            return errorResponse("Failed to get collection items: " + e.getMessage());
        }
    }

    // ==================== Table Tools ====================

    @Tool(name = "get-table-metadata", description = "Get metadata for a specific table including columns, types, and foreign key relationships.")
    public String getTableMetadata(
            @ToolParam(description = "The ID of the table to get metadata for") Integer tableId
    ) {
        log.info("Executing get-table-metadata tool, tableId={}", tableId);
        if (tableId == null) {
            return errorResponse("table_id is required");
        }

        try {
            JsonNode response = webClient.get()
                    .uri("/api/table/" + tableId + "/query_metadata")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Table not found");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("name", response.path("name").asText());
            result.put("schema", response.path("schema").asText());
            result.put("db_id", response.path("db_id").asInt());

            // Extract field information
            List<Map<String, Object>> fields = new ArrayList<>();
            JsonNode fieldsNode = response.path("fields");
            if (fieldsNode.isArray()) {
                for (JsonNode field : fieldsNode) {
                    Map<String, Object> fieldInfo = new LinkedHashMap<>();
                    fieldInfo.put("id", field.path("id").asInt());
                    fieldInfo.put("name", field.path("name").asText());
                    fieldInfo.put("display_name", field.path("display_name").asText());
                    fieldInfo.put("base_type", field.path("base_type").asText());
                    fieldInfo.put("semantic_type", field.path("semantic_type").asText(""));
                    fields.add(fieldInfo);
                }
            }
            result.put("fields", fields);

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to get table metadata {}", tableId, e);
            return errorResponse("Failed to get table metadata: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private String successResponse(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String errorResponse(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (JsonProcessingException e) {
            return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
        }
    }

    private Object nodeToValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                return node.asLong();
            }
            return node.asDouble();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isArray() || node.isObject()) {
            return nodeToMap(node);
        }
        return node.toString();
    }

    private Object nodeToMap(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(node, Object.class);
        } catch (JsonProcessingException e) {
            return node.toString();
        }
    }
}
