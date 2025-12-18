# Task 3.3: Tool Executor Implementation

**Task ID:** 3.3  
**Phase:** 3 - MCP Server Implementation  
**GitHub Issue:** [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)  
**Status:** ⬜ Not Started

---

## Objective

Implement the `MetabaseToolExecutor` service that maps MCP tool calls to actual Metabase API operations using the generated client, handles parameter validation, and transforms responses to MCP format.

---

## Prerequisites

- Task 3.1 completed (Core MCP infrastructure)
- Task 3.2 completed (Tool definitions)
- Phase 2 completed (Metabase API client)
- Understanding of generated API client classes

---

## Technical Details

### Files to Create

1. **Tool Executor Service:** `src/main/java/io/veeblefetzer/remote_mcp_server/service/MetabaseToolExecutor.java`
2. **Tool Result Model:** `src/main/java/io/veeblefetzer/remote_mcp_server/mcp/model/ToolResult.java`
3. **Tool Handlers:** Individual executor methods for each tool

### Architecture

```
┌─────────────────────┐
│  MCP Protocol       │
│  Handler            │
└─────────┬───────────┘
          │ tools/call
          ▼
┌─────────────────────┐
│  Tool Registry      │
│  (validates tool)   │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  MetabaseToolExecutor│
│  (executes tool)    │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Generated Metabase │
│  API Client         │
└─────────────────────┘
```

### MetabaseToolExecutor Service

```java
package io.veeblefetzer.remote_mcp_server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.veeblefetzer.metabase.api.*;
import io.veeblefetzer.metabase.client.ApiClient;
import io.veeblefetzer.remote_mcp_server.mcp.model.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class MetabaseToolExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(MetabaseToolExecutor.class);
    
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;
    
    // API instances (lazily initialized)
    private DatabaseApi databaseApi;
    private CardApi cardApi;
    private DashboardApi dashboardApi;
    private UserApi userApi;
    private CollectionApi collectionApi;
    private DatasetApi datasetApi;
    
    public MetabaseToolExecutor(ApiClient apiClient, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
        initializeApis();
    }
    
    private void initializeApis() {
        this.databaseApi = new DatabaseApi(apiClient);
        this.cardApi = new CardApi(apiClient);
        this.dashboardApi = new DashboardApi(apiClient);
        this.userApi = new UserApi(apiClient);
        this.collectionApi = new CollectionApi(apiClient);
        this.datasetApi = new DatasetApi(apiClient);
    }
    
    /**
     * Execute a tool with the given parameters.
     * Routes to the appropriate handler based on tool name.
     */
    public Mono<ToolResult> execute(String toolName, Map<String, Object> params) {
        log.info("Executing tool: {} with params: {}", toolName, params);
        
        return switch (toolName) {
            case "list-databases" -> executeListDatabases(params);
            case "get-database" -> executeGetDatabase(params);
            case "execute-query" -> executeQuery(params);
            case "list-dashboards" -> executeListDashboards(params);
            case "get-dashboard" -> executeGetDashboard(params);
            case "list-cards" -> executeListCards(params);
            case "get-card" -> executeGetCard(params);
            case "create-card" -> executeCreateCard(params);
            case "get-current-user" -> executeGetCurrentUser(params);
            case "list-collections" -> executeListCollections(params);
            default -> Mono.just(ToolResult.error("Unknown tool: " + toolName));
        };
    }
    
    // Tool implementations below...
}
```

### Tool Execution Methods

#### 1. list-databases

```java
private Mono<ToolResult> executeListDatabases(Map<String, Object> params) {
    boolean includeTables = (Boolean) params.getOrDefault("include_tables", false);
    
    return databaseApi.listDatabases()
        .map(databases -> {
            // Transform to simplified response
            var result = databases.stream()
                .map(db -> Map.of(
                    "id", db.getId(),
                    "name", db.getName(),
                    "engine", db.getEngine(),
                    "is_sample", db.getIsSample()
                ))
                .toList();
            return ToolResult.success(Map.of("databases", result));
        })
        .onErrorResume(e -> {
            log.error("Failed to list databases", e);
            return Mono.just(ToolResult.error("Failed to list databases: " + e.getMessage()));
        });
}
```

#### 2. get-database

```java
private Mono<ToolResult> executeGetDatabase(Map<String, Object> params) {
    Integer databaseId = (Integer) params.get("database_id");
    if (databaseId == null) {
        return Mono.just(ToolResult.error("database_id is required"));
    }
    
    boolean includeMetadata = (Boolean) params.getOrDefault("include_metadata", false);
    
    Mono<Object> databaseMono = databaseApi.getDatabase(databaseId, null)
        .map(db -> (Object) Map.of(
            "id", db.getId(),
            "name", db.getName(),
            "engine", db.getEngine(),
            "details", db.getDetails()
        ));
    
    if (includeMetadata) {
        return databaseApi.getDatabaseMetadata(databaseId, null)
            .map(metadata -> ToolResult.success(Map.of(
                "database", metadata
            )))
            .onErrorResume(e -> Mono.just(ToolResult.error("Failed: " + e.getMessage())));
    }
    
    return databaseMono
        .map(db -> ToolResult.success(Map.of("database", db)))
        .onErrorResume(e -> Mono.just(ToolResult.error("Failed: " + e.getMessage())));
}
```

#### 3. execute-query

```java
private Mono<ToolResult> executeQuery(Map<String, Object> params) {
    Integer databaseId = (Integer) params.get("database_id");
    String query = (String) params.get("query");
    Integer limit = (Integer) params.getOrDefault("limit", 100);
    
    if (databaseId == null || query == null) {
        return Mono.just(ToolResult.error("database_id and query are required"));
    }
    
    // Build native query request
    Map<String, Object> queryRequest = Map.of(
        "database", databaseId,
        "type", "native",
        "native", Map.of(
            "query", query + (query.toLowerCase().contains("limit") ? "" : " LIMIT " + limit)
        )
    );
    
    return datasetApi.queryDatabase(queryRequest)
        .map(result -> {
            // Transform result to tabular format
            var data = result.getData();
            var columns = data.getCols().stream()
                .map(col -> col.getName())
                .toList();
            var rows = data.getRows();
            
            return ToolResult.success(Map.of(
                "columns", columns,
                "rows", rows,
                "row_count", rows.size()
            ));
        })
        .onErrorResume(e -> {
            log.error("Query execution failed", e);
            return Mono.just(ToolResult.error("Query failed: " + e.getMessage()));
        });
}
```

#### 4. list-dashboards

```java
private Mono<ToolResult> executeListDashboards(Map<String, Object> params) {
    Integer collectionId = (Integer) params.get("collection_id");
    
    return dashboardApi.listDashboards()
        .map(dashboards -> {
            var filtered = dashboards.stream()
                .filter(d -> collectionId == null || 
                            collectionId.equals(d.getCollectionId()))
                .map(d -> Map.of(
                    "id", d.getId(),
                    "name", d.getName(),
                    "description", d.getDescription() != null ? d.getDescription() : "",
                    "collection_id", d.getCollectionId()
                ))
                .toList();
            return ToolResult.success(Map.of("dashboards", filtered));
        })
        .onErrorResume(e -> Mono.just(ToolResult.error("Failed: " + e.getMessage())));
}
```

#### 5. get-dashboard

```java
private Mono<ToolResult> executeGetDashboard(Map<String, Object> params) {
    Integer dashboardId = (Integer) params.get("dashboard_id");
    if (dashboardId == null) {
        return Mono.just(ToolResult.error("dashboard_id is required"));
    }
    
    return dashboardApi.getDashboard(dashboardId)
        .map(dashboard -> {
            var cards = dashboard.getOrderedCards().stream()
                .map(card -> Map.of(
                    "id", card.getId(),
                    "card_id", card.getCardId(),
                    "size_x", card.getSizeX(),
                    "size_y", card.getSizeY()
                ))
                .toList();
            
            return ToolResult.success(Map.of(
                "id", dashboard.getId(),
                "name", dashboard.getName(),
                "description", dashboard.getDescription(),
                "cards", cards
            ));
        })
        .onErrorResume(e -> Mono.just(ToolResult.error("Failed: " + e.getMessage())));
}
```

### ToolResult Model

```java
package io.veeblefetzer.remote_mcp_server.mcp.model;

import java.util.List;
import java.util.Map;

public class ToolResult {
    
    private boolean isError;
    private List<ContentBlock> content;
    
    public static ToolResult success(Object data) {
        ToolResult result = new ToolResult();
        result.isError = false;
        result.content = List.of(ContentBlock.text(serializeToJson(data)));
        return result;
    }
    
    public static ToolResult error(String message) {
        ToolResult result = new ToolResult();
        result.isError = true;
        result.content = List.of(ContentBlock.text(message));
        return result;
    }
    
    public Map<String, Object> toMap() {
        return Map.of(
            "isError", isError,
            "content", content.stream()
                .map(ContentBlock::toMap)
                .toList()
        );
    }
    
    // Helper class for content blocks
    public static class ContentBlock {
        private String type;
        private String text;
        
        public static ContentBlock text(String text) {
            ContentBlock block = new ContentBlock();
            block.type = "text";
            block.text = text;
            return block;
        }
        
        public Map<String, Object> toMap() {
            return Map.of("type", type, "text", text);
        }
    }
}
```

---

## Implementation Checklist

- [ ] Create `ToolResult.java` model class
- [ ] Create `MetabaseToolExecutor.java` service
- [ ] Initialize all API client instances
- [ ] Implement routing logic in `execute()` method
- [ ] Implement `executeListDatabases()`
- [ ] Implement `executeGetDatabase()`
- [ ] Implement `executeQuery()`
- [ ] Implement `executeListDashboards()`
- [ ] Implement `executeGetDashboard()`
- [ ] Implement `executeListCards()`
- [ ] Implement `executeGetCard()`
- [ ] Implement `executeCreateCard()`
- [ ] Implement `executeGetCurrentUser()`
- [ ] Implement `executeListCollections()`
- [ ] Add parameter validation for each tool
- [ ] Add comprehensive error handling
- [ ] Add logging for debugging
- [ ] Update `McpProtocolHandler.handleCallTool()`
- [ ] Test each tool execution manually

---

## Acceptance Criteria

- [ ] All 10 tools execute successfully
- [ ] Parameter validation returns clear error messages
- [ ] API errors are properly caught and reported
- [ ] Responses are formatted according to MCP spec
- [ ] ToolResult contains proper content blocks
- [ ] Logging captures tool execution details
- [ ] Code compiles without errors
- [ ] Integration test verifies basic tool execution

---

## Update McpProtocolHandler

```java
// In McpProtocolHandler.java
@Autowired
private MetabaseToolExecutor toolExecutor;

private Mono<JsonRpcResponse> handleCallTool(JsonRpcRequest request) {
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) request.getParams();
    
    String toolName = (String) params.get("name");
    @SuppressWarnings("unchecked")
    Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
    
    if (toolName == null) {
        return Mono.just(JsonRpcResponse.error(
            request.getId(),
            JsonRpcError.INVALID_PARAMS,
            "Tool name is required"
        ));
    }
    
    if (!toolRegistry.hasTool(toolName)) {
        return Mono.just(JsonRpcResponse.error(
            request.getId(),
            JsonRpcError.METHOD_NOT_FOUND,
            "Unknown tool: " + toolName
        ));
    }
    
    return toolExecutor.execute(toolName, arguments != null ? arguments : Map.of())
        .map(result -> JsonRpcResponse.success(request.getId(), result.toMap()))
        .onErrorResume(e -> Mono.just(JsonRpcResponse.error(
            request.getId(),
            JsonRpcError.INTERNAL_ERROR,
            "Tool execution failed: " + e.getMessage()
        )));
}
```

---

## Error Handling Strategy

| Error Type | Response |
|------------|----------|
| Missing required parameter | `ToolResult.error("parameter_name is required")` |
| Invalid parameter type | `ToolResult.error("parameter_name must be integer")` |
| Metabase API error | `ToolResult.error("API error: " + message)` |
| Network error | `ToolResult.error("Connection failed: " + message)` |
| Unexpected error | `ToolResult.error("Internal error: " + message)` |

---

## Testing Tools Manually

```bash
# Test list-databases
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "list-databases",
      "arguments": {}
    }
  }'

# Test execute-query
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "execute-query",
      "arguments": {
        "database_id": 1,
        "query": "SELECT * FROM customers LIMIT 10"
      }
    }
  }'
```

---

## Commit Template

```
feat(executor): implement MetabaseToolExecutor service

- Create ToolResult model for MCP-compliant responses
- Implement MetabaseToolExecutor with all 10 tool handlers:
  - Database operations: list-databases, get-database
  - Query execution: execute-query
  - Dashboard operations: list-dashboards, get-dashboard
  - Card operations: list-cards, get-card, create-card
  - User operations: get-current-user
  - Collection operations: list-collections
- Add parameter validation and error handling
- Update McpProtocolHandler to use executor
- Add comprehensive logging for debugging

Part of #3
```

---

## Notes

- Each tool handler should be self-contained
- Transform Metabase API responses to simpler structures
- Handle null values gracefully (Metabase API often returns nulls)
- Consider response size limits for large datasets
- Log enough detail for debugging but avoid sensitive data
- Use `@SuppressWarnings("unchecked")` sparingly for Map casts
