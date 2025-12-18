package io.veeblefetzer.remote_mcp_server.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing MCP resources for Metabase data.
 * Resources provide read-only access to Metabase configuration and metadata.
 */
@Service
public class MetabaseResourceService {

    private static final Logger log = LoggerFactory.getLogger(MetabaseResourceService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Resource URI patterns
    private static final Pattern DATABASE_PATTERN = Pattern.compile("metabase://database/(\\d+)");
    private static final Pattern DATABASE_TABLES_PATTERN = Pattern.compile("metabase://database/(\\d+)/tables");
    private static final Pattern DASHBOARD_PATTERN = Pattern.compile("metabase://dashboard/(\\d+)");
    private static final Pattern CARD_PATTERN = Pattern.compile("metabase://card/(\\d+)");
    private static final Pattern COLLECTION_PATTERN = Pattern.compile("metabase://collection/(\\d+|root)");

    public MetabaseResourceService(WebClient metabaseWebClient, ObjectMapper objectMapper) {
        this.webClient = metabaseWebClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        log.info("Metabase Resource Service initialized");
        log.info("Resource templates available:");
        log.info("  metabase://database/{id}         - Database metadata");
        log.info("  metabase://database/{id}/tables  - Database tables");
        log.info("  metabase://dashboard/{id}        - Dashboard configuration");
        log.info("  metabase://card/{id}             - Card/question definition");
        log.info("  metabase://collection/{id}       - Collection contents");
    }

    /**
     * Lists all available resource templates.
     *
     * @return list of resource template definitions
     */
    public List<Map<String, Object>> listResourceTemplates() {
        return List.of(
            createResourceTemplate(
                "metabase://database/{id}",
                "Database Metadata",
                "Provides metadata about a Metabase database, including name, engine type, and connection status.",
                "application/json"
            ),
            createResourceTemplate(
                "metabase://database/{id}/tables",
                "Database Tables",
                "Lists all tables in a database with their schemas, including column names, types, and relationships.",
                "application/json"
            ),
            createResourceTemplate(
                "metabase://dashboard/{id}",
                "Dashboard Configuration",
                "Provides the complete configuration of a dashboard, including layout, cards, and filter settings.",
                "application/json"
            ),
            createResourceTemplate(
                "metabase://card/{id}",
                "Card/Question Definition",
                "Provides the definition of a saved question (card), including the query, visualization settings, and description.",
                "application/json"
            ),
            createResourceTemplate(
                "metabase://collection/{id}",
                "Collection Contents",
                "Lists all items in a collection, including dashboards, questions, and sub-collections.",
                "application/json"
            )
        );
    }

    private Map<String, Object> createResourceTemplate(String uriTemplate, String name, 
                                                        String description, String mimeType) {
        return Map.of(
            "uriTemplate", uriTemplate,
            "name", name,
            "description", description,
            "mimeType", mimeType
        );
    }

    /**
     * Resolves a resource URI and returns its content.
     *
     * @param uri the resource URI to resolve
     * @return the resource content as a string
     */
    public String resolveResource(String uri) {
        log.info("Resolving resource: {}", uri);

        try {
            // Try matching each pattern
            Matcher matcher;

            // Database tables (must be checked before database)
            matcher = DATABASE_TABLES_PATTERN.matcher(uri);
            if (matcher.matches()) {
                return resolveDatabaseTables(Integer.parseInt(matcher.group(1)));
            }

            // Database
            matcher = DATABASE_PATTERN.matcher(uri);
            if (matcher.matches()) {
                return resolveDatabase(Integer.parseInt(matcher.group(1)));
            }

            // Dashboard
            matcher = DASHBOARD_PATTERN.matcher(uri);
            if (matcher.matches()) {
                return resolveDashboard(Integer.parseInt(matcher.group(1)));
            }

            // Card
            matcher = CARD_PATTERN.matcher(uri);
            if (matcher.matches()) {
                return resolveCard(Integer.parseInt(matcher.group(1)));
            }

            // Collection
            matcher = COLLECTION_PATTERN.matcher(uri);
            if (matcher.matches()) {
                return resolveCollection(matcher.group(1));
            }

            return errorResponse("Unknown resource URI: " + uri);
        } catch (Exception e) {
            log.error("Failed to resolve resource: {}", uri, e);
            return errorResponse("Failed to resolve resource: " + e.getMessage());
        }
    }

    private String resolveDatabase(int databaseId) {
        try {
            JsonNode response = webClient.get()
                    .uri("/api/database/" + databaseId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Database not found: " + databaseId);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("name", response.path("name").asText());
            result.put("engine", response.path("engine").asText());
            result.put("description", response.path("description").asText(""));
            result.put("is_sample", response.path("is_sample").asBoolean());
            result.put("created_at", response.path("created_at").asText());
            result.put("updated_at", response.path("updated_at").asText());

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to resolve database {}", databaseId, e);
            return errorResponse("Failed to get database: " + e.getMessage());
        }
    }

    private String resolveDatabaseTables(int databaseId) {
        try {
            JsonNode response = webClient.get()
                    .uri("/api/database/" + databaseId + "/metadata")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Database metadata not found: " + databaseId);
            }

            List<Map<String, Object>> tables = new ArrayList<>();
            JsonNode tablesNode = response.path("tables");
            if (tablesNode.isArray()) {
                for (JsonNode table : tablesNode) {
                    Map<String, Object> tableInfo = new LinkedHashMap<>();
                    tableInfo.put("id", table.path("id").asInt());
                    tableInfo.put("name", table.path("name").asText());
                    tableInfo.put("schema", table.path("schema").asText());
                    tableInfo.put("display_name", table.path("display_name").asText());
                    tableInfo.put("description", table.path("description").asText(""));

                    // Extract field information
                    List<Map<String, Object>> fields = new ArrayList<>();
                    JsonNode fieldsNode = table.path("fields");
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
                    tableInfo.put("fields", fields);
                    tables.add(tableInfo);
                }
            }

            return successResponse(Map.of(
                "database_id", databaseId,
                "tables", tables
            ));
        } catch (Exception e) {
            log.error("Failed to resolve database tables {}", databaseId, e);
            return errorResponse("Failed to get database tables: " + e.getMessage());
        }
    }

    private String resolveDashboard(int dashboardId) {
        try {
            JsonNode response = webClient.get()
                    .uri("/api/dashboard/" + dashboardId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Dashboard not found: " + dashboardId);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("name", response.path("name").asText());
            result.put("description", response.path("description").asText(""));
            result.put("collection_id", response.has("collection_id") && !response.get("collection_id").isNull() 
                    ? response.get("collection_id").asInt() : null);
            result.put("created_at", response.path("created_at").asText());
            result.put("updated_at", response.path("updated_at").asText());

            // Extract dashboard cards
            List<Map<String, Object>> cards = new ArrayList<>();
            JsonNode dashcardsNode = response.path("dashcards");
            if (dashcardsNode.isArray()) {
                for (JsonNode dashcard : dashcardsNode) {
                    Map<String, Object> cardInfo = new LinkedHashMap<>();
                    cardInfo.put("id", dashcard.path("id").asInt());
                    cardInfo.put("card_id", dashcard.has("card_id") && !dashcard.get("card_id").isNull() 
                            ? dashcard.get("card_id").asInt() : null);
                    cardInfo.put("row", dashcard.path("row").asInt());
                    cardInfo.put("col", dashcard.path("col").asInt());
                    cardInfo.put("size_x", dashcard.path("size_x").asInt());
                    cardInfo.put("size_y", dashcard.path("size_y").asInt());
                    cards.add(cardInfo);
                }
            }
            result.put("dashcards", cards);

            // Extract parameters
            result.put("parameters", nodeToObject(response.path("parameters")));

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to resolve dashboard {}", dashboardId, e);
            return errorResponse("Failed to get dashboard: " + e.getMessage());
        }
    }

    private String resolveCard(int cardId) {
        try {
            JsonNode response = webClient.get()
                    .uri("/api/card/" + cardId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Card not found: " + cardId);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", response.path("id").asInt());
            result.put("name", response.path("name").asText());
            result.put("description", response.path("description").asText(""));
            result.put("database_id", response.has("database_id") && !response.get("database_id").isNull() 
                    ? response.get("database_id").asInt() : null);
            result.put("collection_id", response.has("collection_id") && !response.get("collection_id").isNull() 
                    ? response.get("collection_id").asInt() : null);
            result.put("query_type", response.path("query_type").asText());
            result.put("display", response.path("display").asText());
            result.put("dataset_query", nodeToObject(response.path("dataset_query")));
            result.put("visualization_settings", nodeToObject(response.path("visualization_settings")));
            result.put("created_at", response.path("created_at").asText());
            result.put("updated_at", response.path("updated_at").asText());

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to resolve card {}", cardId, e);
            return errorResponse("Failed to get card: " + e.getMessage());
        }
    }

    private String resolveCollection(String collectionId) {
        try {
            JsonNode response = webClient.get()
                    .uri("/api/collection/" + collectionId + "/items")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return errorResponse("Collection not found: " + collectionId);
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

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("collection_id", collectionId);
            result.put("items", items);
            result.put("total", response.has("total") ? response.get("total").asInt() : items.size());

            return successResponse(result);
        } catch (Exception e) {
            log.error("Failed to resolve collection {}", collectionId, e);
            return errorResponse("Failed to get collection: " + e.getMessage());
        }
    }

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

    private Object nodeToObject(JsonNode node) {
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
