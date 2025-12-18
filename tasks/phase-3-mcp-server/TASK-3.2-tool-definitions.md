# Task 3.2: Tool Definitions

**Task ID:** 3.2  
**Phase:** 3 - MCP Server Implementation  
**GitHub Issue:** [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)  
**Status:** ⬜ Not Started

---

## Objective

Define all MCP tools that expose Metabase API functionality, including tool names, descriptions, input schemas, and output formats.

---

## Prerequisites

- Task 3.1 completed (Core MCP infrastructure)
- Understanding of MCP tool specification format
- Knowledge of Metabase API endpoints

---

## Technical Details

### Tool Definition Structure

Each MCP tool requires:
- **name**: Tool identifier (kebab-case)
- **description**: Human-readable description for AI assistants
- **inputSchema**: JSON Schema defining expected parameters
- **output format**: Structure of the response

### Files to Create

1. **Tool Registry:** `src/main/java/io/veeblefetzer/remote_mcp_server/tools/ToolRegistry.java`
2. **Tool Definition Model:** `src/main/java/io/veeblefetzer/remote_mcp_server/tools/ToolDefinition.java`
3. **Individual Tool Classes:** One per tool in `src/main/java/io/veeblefetzer/remote_mcp_server/tools/definitions/`

### Tool Categories

#### 1. Database Tools
| Tool Name | Description | Metabase API |
|-----------|-------------|--------------|
| `list-databases` | List all databases connected to Metabase | GET /api/database |
| `get-database` | Get details of a specific database | GET /api/database/{id} |
| `get-database-metadata` | Get table and field metadata | GET /api/database/{id}/metadata |

#### 2. Query Tools
| Tool Name | Description | Metabase API |
|-----------|-------------|--------------|
| `execute-query` | Execute a native SQL query | POST /api/dataset |
| `get-query-results` | Get results of a saved question | POST /api/card/{id}/query |

#### 3. Dashboard Tools
| Tool Name | Description | Metabase API |
|-----------|-------------|--------------|
| `list-dashboards` | List all dashboards | GET /api/dashboard |
| `get-dashboard` | Get dashboard details and cards | GET /api/dashboard/{id} |

#### 4. Card/Question Tools
| Tool Name | Description | Metabase API |
|-----------|-------------|--------------|
| `list-cards` | List all saved questions | GET /api/card |
| `get-card` | Get card/question details | GET /api/card/{id} |
| `create-card` | Create a new question | POST /api/card |

#### 5. User Tools
| Tool Name | Description | Metabase API |
|-----------|-------------|--------------|
| `get-current-user` | Get current authenticated user | GET /api/user/current |

---

## Tool Definitions

### 1. list-databases

```java
package io.veeblefetzer.remote_mcp_server.tools.definitions;

import io.veeblefetzer.remote_mcp_server.tools.ToolDefinition;
import java.util.Map;

public class ListDatabasesTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "list-databases";
    }
    
    @Override
    public String getDescription() {
        return "List all databases connected to Metabase. Returns database IDs, names, " +
               "and connection details. Use this to discover available data sources.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "include_tables", Map.of(
                    "type", "boolean",
                    "description", "Include table information in response",
                    "default", false
                )
            ),
            "required", List.of()
        );
    }
}
```

### 2. get-database

```java
public class GetDatabaseTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "get-database";
    }
    
    @Override
    public String getDescription() {
        return "Get detailed information about a specific database, including " +
               "connection settings and available tables.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "database_id", Map.of(
                    "type", "integer",
                    "description", "The ID of the database to retrieve"
                ),
                "include_metadata", Map.of(
                    "type", "boolean",
                    "description", "Include table and field metadata",
                    "default", false
                )
            ),
            "required", List.of("database_id")
        );
    }
}
```

### 3. execute-query

```java
public class ExecuteQueryTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "execute-query";
    }
    
    @Override
    public String getDescription() {
        return "Execute a native SQL query against a database. Returns query results " +
               "as rows and columns. Use with caution - queries are executed directly.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "database_id", Map.of(
                    "type", "integer",
                    "description", "The database ID to query"
                ),
                "query", Map.of(
                    "type", "string",
                    "description", "The SQL query to execute"
                ),
                "limit", Map.of(
                    "type", "integer",
                    "description", "Maximum number of rows to return",
                    "default", 100
                )
            ),
            "required", List.of("database_id", "query")
        );
    }
}
```

### 4. list-dashboards

```java
public class ListDashboardsTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "list-dashboards";
    }
    
    @Override
    public String getDescription() {
        return "List all dashboards in Metabase. Returns dashboard IDs, names, " +
               "and descriptions. Use to discover available dashboards.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "collection_id", Map.of(
                    "type", "integer",
                    "description", "Filter by collection ID (optional)"
                )
            ),
            "required", List.of()
        );
    }
}
```

### 5. get-dashboard

```java
public class GetDashboardTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "get-dashboard";
    }
    
    @Override
    public String getDescription() {
        return "Get detailed information about a dashboard, including all cards " +
               "(questions) and their configurations.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "dashboard_id", Map.of(
                    "type", "integer",
                    "description", "The ID of the dashboard to retrieve"
                )
            ),
            "required", List.of("dashboard_id")
        );
    }
}
```

### 6. list-cards

```java
public class ListCardsTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "list-cards";
    }
    
    @Override
    public String getDescription() {
        return "List all saved questions (cards) in Metabase. Returns card IDs, " +
               "names, and query types.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "collection_id", Map.of(
                    "type", "integer",
                    "description", "Filter by collection ID (optional)"
                ),
                "database_id", Map.of(
                    "type", "integer",
                    "description", "Filter by database ID (optional)"
                )
            ),
            "required", List.of()
        );
    }
}
```

### 7. get-card

```java
public class GetCardTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "get-card";
    }
    
    @Override
    public String getDescription() {
        return "Get detailed information about a saved question (card), including " +
               "the query definition and visualization settings.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "card_id", Map.of(
                    "type", "integer",
                    "description", "The ID of the card to retrieve"
                )
            ),
            "required", List.of("card_id")
        );
    }
}
```

### 8. create-card

```java
public class CreateCardTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "create-card";
    }
    
    @Override
    public String getDescription() {
        return "Create a new saved question (card) in Metabase. Requires query " +
               "definition and visualization settings.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "name", Map.of(
                    "type", "string",
                    "description", "Name of the question"
                ),
                "database_id", Map.of(
                    "type", "integer",
                    "description", "Database to query"
                ),
                "query", Map.of(
                    "type", "string",
                    "description", "SQL query or MBQL query string"
                ),
                "visualization_type", Map.of(
                    "type", "string",
                    "description", "Type of visualization (table, bar, line, etc.)",
                    "default", "table"
                ),
                "collection_id", Map.of(
                    "type", "integer",
                    "description", "Collection to save the card in (optional)"
                )
            ),
            "required", List.of("name", "database_id", "query")
        );
    }
}
```

### 9. get-current-user

```java
public class GetCurrentUserTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "get-current-user";
    }
    
    @Override
    public String getDescription() {
        return "Get information about the currently authenticated user, including " +
               "name, email, and permissions.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", List.of()
        );
    }
}
```

### 10. list-collections

```java
public class ListCollectionsTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "list-collections";
    }
    
    @Override
    public String getDescription() {
        return "List all collections in Metabase. Collections organize dashboards " +
               "and questions into folders.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "parent_id", Map.of(
                    "type", "integer",
                    "description", "Parent collection ID to filter by (optional)"
                )
            ),
            "required", List.of()
        );
    }
}
```

---

## Tool Registry Implementation

```java
package io.veeblefetzer.remote_mcp_server.tools;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ToolRegistry {
    
    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
    
    public ToolRegistry(List<ToolDefinition> toolDefinitions) {
        for (ToolDefinition tool : toolDefinitions) {
            tools.put(tool.getName(), tool);
        }
    }
    
    public List<Map<String, Object>> listTools() {
        return tools.values().stream()
            .map(tool -> Map.<String, Object>of(
                "name", tool.getName(),
                "description", tool.getDescription(),
                "inputSchema", tool.getInputSchema()
            ))
            .toList();
    }
    
    public Optional<ToolDefinition> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
    
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
```

---

## Implementation Checklist

- [ ] Create `ToolDefinition.java` interface
- [ ] Create `ToolRegistry.java` component
- [ ] Create `definitions` package for tool classes
- [ ] Implement `ListDatabasesTool`
- [ ] Implement `GetDatabaseTool`
- [ ] Implement `ExecuteQueryTool`
- [ ] Implement `ListDashboardsTool`
- [ ] Implement `GetDashboardTool`
- [ ] Implement `ListCardsTool`
- [ ] Implement `GetCardTool`
- [ ] Implement `CreateCardTool`
- [ ] Implement `GetCurrentUserTool`
- [ ] Implement `ListCollectionsTool`
- [ ] Update `McpProtocolHandler` to use `ToolRegistry`
- [ ] Test: `tools/list` returns all 10 tools
- [ ] Test: Each tool has valid JSON schema
- [ ] Verify tool descriptions are clear and helpful

---

## Acceptance Criteria

- [ ] 10 tool definitions implemented
- [ ] All tools registered in `ToolRegistry`
- [ ] `tools/list` MCP method returns all tools
- [ ] Each tool has name, description, and valid inputSchema
- [ ] Required parameters are correctly marked
- [ ] Tool descriptions are AI-friendly (clear, actionable)
- [ ] Code compiles without errors

---

## Update McpProtocolHandler

```java
// In McpProtocolHandler.java
@Autowired
private ToolRegistry toolRegistry;

private Mono<JsonRpcResponse> handleListTools(JsonRpcRequest request) {
    Map<String, Object> result = Map.of(
        "tools", toolRegistry.listTools()
    );
    return Mono.just(JsonRpcResponse.success(request.getId(), result));
}
```

---

## Commit Template

```
feat(tools): define MCP tool specifications

- Create ToolDefinition interface and ToolRegistry component
- Implement 10 Metabase tools:
  - Database tools: list-databases, get-database
  - Query tools: execute-query
  - Dashboard tools: list-dashboards, get-dashboard
  - Card tools: list-cards, get-card, create-card
  - User tools: get-current-user
  - Collection tools: list-collections
- Add JSON Schema definitions for all tool inputs
- Update McpProtocolHandler to use ToolRegistry

Part of #3
```

---

## Notes

- Tool names use kebab-case per MCP convention
- Descriptions should help AI understand when to use each tool
- Input schemas use JSON Schema draft-07 format
- Keep parameter descriptions concise but informative
- Mark truly required parameters as required
- Provide sensible defaults where appropriate
