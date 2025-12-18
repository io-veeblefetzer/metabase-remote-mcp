# Task 3.4: Resource Implementation

**Task ID:** 3.4  
**Phase:** 3 - MCP Server Implementation  
**GitHub Issue:** [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)  
**Status:** ⬜ Not Started

---

## Objective

Implement MCP resources that expose Metabase data as context for AI assistants, allowing them to read database metadata, dashboard configurations, and card definitions without executing tools.

---

## Prerequisites

- Task 3.1 completed (Core MCP infrastructure)
- Phase 2 completed (Metabase API client)
- Understanding of MCP resource specification

---

## Technical Details

### MCP Resources Overview

Resources provide read-only access to data that AI assistants can use as context. Unlike tools, resources are passive - they don't perform actions but provide information.

### URI Scheme

All resources use the `metabase://` URI scheme:

```
metabase://database/{id}           - Database metadata
metabase://database/{id}/tables    - Database tables list
metabase://dashboard/{id}          - Dashboard configuration
metabase://card/{id}               - Card/question definition
metabase://collection/{id}         - Collection contents
```

### Files to Create

1. **Resource Registry:** `src/main/java/io/veeblefetzer/remote_mcp_server/resources/ResourceRegistry.java`
2. **Resource Definition:** `src/main/java/io/veeblefetzer/remote_mcp_server/resources/ResourceDefinition.java`
3. **Resource Resolver:** `src/main/java/io/veeblefetzer/remote_mcp_server/resources/ResourceResolver.java`
4. **Individual Resources:** `src/main/java/io/veeblefetzer/remote_mcp_server/resources/definitions/`

---

## Resource Definitions

### 1. Database Resource

```java
package io.veeblefetzer.remote_mcp_server.resources.definitions;

import io.veeblefetzer.remote_mcp_server.resources.ResourceDefinition;

public class DatabaseResource implements ResourceDefinition {
    
    @Override
    public String getUriTemplate() {
        return "metabase://database/{id}";
    }
    
    @Override
    public String getName() {
        return "Database Metadata";
    }
    
    @Override
    public String getDescription() {
        return "Provides metadata about a Metabase database, including name, " +
               "engine type, and connection status.";
    }
    
    @Override
    public String getMimeType() {
        return "application/json";
    }
}
```

### 2. Database Tables Resource

```java
public class DatabaseTablesResource implements ResourceDefinition {
    
    @Override
    public String getUriTemplate() {
        return "metabase://database/{id}/tables";
    }
    
    @Override
    public String getName() {
        return "Database Tables";
    }
    
    @Override
    public String getDescription() {
        return "Lists all tables in a database with their schemas, including " +
               "column names, types, and relationships.";
    }
    
    @Override
    public String getMimeType() {
        return "application/json";
    }
}
```

### 3. Dashboard Resource

```java
public class DashboardResource implements ResourceDefinition {
    
    @Override
    public String getUriTemplate() {
        return "metabase://dashboard/{id}";
    }
    
    @Override
    public String getName() {
        return "Dashboard Configuration";
    }
    
    @Override
    public String getDescription() {
        return "Provides the complete configuration of a dashboard, including " +
               "layout, cards, and filter settings.";
    }
    
    @Override
    public String getMimeType() {
        return "application/json";
    }
}
```

### 4. Card Resource

```java
public class CardResource implements ResourceDefinition {
    
    @Override
    public String getUriTemplate() {
        return "metabase://card/{id}";
    }
    
    @Override
    public String getName() {
        return "Card/Question Definition";
    }
    
    @Override
    public String getDescription() {
        return "Provides the definition of a saved question (card), including " +
               "the query, visualization settings, and description.";
    }
    
    @Override
    public String getMimeType() {
        return "application/json";
    }
}
```

### 5. Collection Resource

```java
public class CollectionResource implements ResourceDefinition {
    
    @Override
    public String getUriTemplate() {
        return "metabase://collection/{id}";
    }
    
    @Override
    public String getName() {
        return "Collection Contents";
    }
    
    @Override
    public String getDescription() {
        return "Lists all items in a collection, including dashboards, " +
               "questions, and sub-collections.";
    }
    
    @Override
    public String getMimeType() {
        return "application/json";
    }
}
```

---

## Resource Registry Implementation

```java
package io.veeblefetzer.remote_mcp_server.resources;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResourceRegistry {
    
    private final List<ResourceDefinition> resources;
    private final Map<Pattern, ResourceDefinition> patterns;
    
    public ResourceRegistry(List<ResourceDefinition> resourceDefinitions) {
        this.resources = resourceDefinitions;
        this.patterns = new LinkedHashMap<>();
        
        for (ResourceDefinition resource : resourceDefinitions) {
            Pattern pattern = uriTemplateToPattern(resource.getUriTemplate());
            patterns.put(pattern, resource);
        }
    }
    
    /**
     * List all resource templates for MCP resources/list
     */
    public List<Map<String, Object>> listResources() {
        return resources.stream()
            .map(r -> Map.<String, Object>of(
                "uri", r.getUriTemplate(),
                "name", r.getName(),
                "description", r.getDescription(),
                "mimeType", r.getMimeType()
            ))
            .toList();
    }
    
    /**
     * Find a resource definition matching the given URI
     */
    public Optional<ResourceMatch> matchUri(String uri) {
        for (Map.Entry<Pattern, ResourceDefinition> entry : patterns.entrySet()) {
            Matcher matcher = entry.getKey().matcher(uri);
            if (matcher.matches()) {
                Map<String, String> params = extractParams(
                    entry.getValue().getUriTemplate(), 
                    matcher
                );
                return Optional.of(new ResourceMatch(entry.getValue(), params));
            }
        }
        return Optional.empty();
    }
    
    private Pattern uriTemplateToPattern(String template) {
        // Convert {id} style templates to regex groups
        String regex = template
            .replace("{id}", "(?<id>\\d+)")
            .replace("://", "\\:\\/\\/");
        return Pattern.compile(regex);
    }
    
    private Map<String, String> extractParams(String template, Matcher matcher) {
        Map<String, String> params = new HashMap<>();
        if (template.contains("{id}")) {
            params.put("id", matcher.group("id"));
        }
        return params;
    }
    
    public static class ResourceMatch {
        private final ResourceDefinition definition;
        private final Map<String, String> params;
        
        public ResourceMatch(ResourceDefinition definition, Map<String, String> params) {
            this.definition = definition;
            this.params = params;
        }
        
        public ResourceDefinition getDefinition() { return definition; }
        public Map<String, String> getParams() { return params; }
    }
}
```

---

## Resource Resolver Implementation

```java
package io.veeblefetzer.remote_mcp_server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.veeblefetzer.metabase.api.*;
import io.veeblefetzer.metabase.client.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class ResourceResolver {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceResolver.class);
    
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final ResourceRegistry resourceRegistry;
    
    private DatabaseApi databaseApi;
    private DashboardApi dashboardApi;
    private CardApi cardApi;
    private CollectionApi collectionApi;
    
    public ResourceResolver(ApiClient apiClient, 
                           ObjectMapper objectMapper,
                           ResourceRegistry resourceRegistry) {
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
        this.resourceRegistry = resourceRegistry;
        initializeApis();
    }
    
    private void initializeApis() {
        this.databaseApi = new DatabaseApi(apiClient);
        this.dashboardApi = new DashboardApi(apiClient);
        this.cardApi = new CardApi(apiClient);
        this.collectionApi = new CollectionApi(apiClient);
    }
    
    /**
     * Resolve a resource URI and return its contents
     */
    public Mono<ResourceContent> resolve(String uri) {
        log.info("Resolving resource: {}", uri);
        
        return resourceRegistry.matchUri(uri)
            .map(match -> resolveMatch(uri, match))
            .orElse(Mono.just(ResourceContent.error("Unknown resource URI: " + uri)));
    }
    
    private Mono<ResourceContent> resolveMatch(String uri, 
                                               ResourceRegistry.ResourceMatch match) {
        String template = match.getDefinition().getUriTemplate();
        Map<String, String> params = match.getParams();
        
        return switch (template) {
            case "metabase://database/{id}" -> 
                resolveDatabase(Integer.parseInt(params.get("id")));
            case "metabase://database/{id}/tables" -> 
                resolveDatabaseTables(Integer.parseInt(params.get("id")));
            case "metabase://dashboard/{id}" -> 
                resolveDashboard(Integer.parseInt(params.get("id")));
            case "metabase://card/{id}" -> 
                resolveCard(Integer.parseInt(params.get("id")));
            case "metabase://collection/{id}" -> 
                resolveCollection(Integer.parseInt(params.get("id")));
            default -> 
                Mono.just(ResourceContent.error("Unhandled resource: " + template));
        };
    }
    
    private Mono<ResourceContent> resolveDatabase(int id) {
        return databaseApi.getDatabase(id, null)
            .map(db -> ResourceContent.success(
                "metabase://database/" + id,
                "application/json",
                serializeToJson(Map.of(
                    "id", db.getId(),
                    "name", db.getName(),
                    "engine", db.getEngine(),
                    "details", db.getDetails()
                ))
            ))
            .onErrorResume(e -> Mono.just(ResourceContent.error(e.getMessage())));
    }
    
    private Mono<ResourceContent> resolveDatabaseTables(int id) {
        return databaseApi.getDatabaseMetadata(id, null)
            .map(metadata -> {
                var tables = metadata.getTables().stream()
                    .map(t -> Map.of(
                        "id", t.getId(),
                        "name", t.getName(),
                        "schema", t.getSchema(),
                        "fields", t.getFields().stream()
                            .map(f -> Map.of(
                                "id", f.getId(),
                                "name", f.getName(),
                                "type", f.getBaseType()
                            ))
                            .toList()
                    ))
                    .toList();
                return ResourceContent.success(
                    "metabase://database/" + id + "/tables",
                    "application/json",
                    serializeToJson(Map.of("tables", tables))
                );
            })
            .onErrorResume(e -> Mono.just(ResourceContent.error(e.getMessage())));
    }
    
    private Mono<ResourceContent> resolveDashboard(int id) {
        return dashboardApi.getDashboard(id)
            .map(dashboard -> ResourceContent.success(
                "metabase://dashboard/" + id,
                "application/json",
                serializeToJson(dashboard)
            ))
            .onErrorResume(e -> Mono.just(ResourceContent.error(e.getMessage())));
    }
    
    private Mono<ResourceContent> resolveCard(int id) {
        return cardApi.getCard(id)
            .map(card -> ResourceContent.success(
                "metabase://card/" + id,
                "application/json",
                serializeToJson(card)
            ))
            .onErrorResume(e -> Mono.just(ResourceContent.error(e.getMessage())));
    }
    
    private Mono<ResourceContent> resolveCollection(int id) {
        return collectionApi.getCollectionItems(id, null)
            .map(items -> ResourceContent.success(
                "metabase://collection/" + id,
                "application/json",
                serializeToJson(Map.of("items", items))
            ))
            .onErrorResume(e -> Mono.just(ResourceContent.error(e.getMessage())));
    }
    
    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

---

## ResourceContent Model

```java
package io.veeblefetzer.remote_mcp_server.resources;

import java.util.List;
import java.util.Map;

public class ResourceContent {
    
    private String uri;
    private String mimeType;
    private String text;
    private boolean isError;
    private String errorMessage;
    
    public static ResourceContent success(String uri, String mimeType, String text) {
        ResourceContent content = new ResourceContent();
        content.uri = uri;
        content.mimeType = mimeType;
        content.text = text;
        content.isError = false;
        return content;
    }
    
    public static ResourceContent error(String message) {
        ResourceContent content = new ResourceContent();
        content.isError = true;
        content.errorMessage = message;
        return content;
    }
    
    public Map<String, Object> toMap() {
        if (isError) {
            return Map.of("error", errorMessage);
        }
        return Map.of(
            "contents", List.of(Map.of(
                "uri", uri,
                "mimeType", mimeType,
                "text", text
            ))
        );
    }
    
    // Getters...
}
```

---

## Update McpProtocolHandler

```java
// In McpProtocolHandler.java
@Autowired
private ResourceRegistry resourceRegistry;

@Autowired
private ResourceResolver resourceResolver;

private Mono<JsonRpcResponse> handleListResources(JsonRpcRequest request) {
    Map<String, Object> result = Map.of(
        "resources", resourceRegistry.listResources()
    );
    return Mono.just(JsonRpcResponse.success(request.getId(), result));
}

private Mono<JsonRpcResponse> handleReadResource(JsonRpcRequest request) {
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) request.getParams();
    String uri = (String) params.get("uri");
    
    if (uri == null) {
        return Mono.just(JsonRpcResponse.error(
            request.getId(),
            JsonRpcError.INVALID_PARAMS,
            "Resource URI is required"
        ));
    }
    
    return resourceResolver.resolve(uri)
        .map(content -> JsonRpcResponse.success(request.getId(), content.toMap()))
        .onErrorResume(e -> Mono.just(JsonRpcResponse.error(
            request.getId(),
            JsonRpcError.INTERNAL_ERROR,
            "Failed to read resource: " + e.getMessage()
        )));
}
```

---

## Implementation Checklist

- [ ] Create `ResourceDefinition.java` interface
- [ ] Create `ResourceRegistry.java` component
- [ ] Implement URI template to regex conversion
- [ ] Create `ResourceContent.java` model
- [ ] Create `ResourceResolver.java` service
- [ ] Implement `DatabaseResource`
- [ ] Implement `DatabaseTablesResource`
- [ ] Implement `DashboardResource`
- [ ] Implement `CardResource`
- [ ] Implement `CollectionResource`
- [ ] Implement `resolveDatabase()` method
- [ ] Implement `resolveDatabaseTables()` method
- [ ] Implement `resolveDashboard()` method
- [ ] Implement `resolveCard()` method
- [ ] Implement `resolveCollection()` method
- [ ] Update `McpProtocolHandler` for resources
- [ ] Test: `resources/list` returns all templates
- [ ] Test: `resources/read` resolves each resource type

---

## Acceptance Criteria

- [ ] 5 resource types implemented
- [ ] `resources/list` returns all resource templates
- [ ] `resources/read` resolves valid URIs
- [ ] Invalid URIs return appropriate error
- [ ] Resource contents are JSON formatted
- [ ] MIME types are correctly set
- [ ] Code compiles without errors

---

## Testing Resources

```bash
# List resources
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "resources/list",
    "params": {}
  }'

# Read a database resource
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "resources/read",
    "params": {
      "uri": "metabase://database/1"
    }
  }'
```

---

## Commit Template

```
feat(resources): implement MCP resources for Metabase

- Create ResourceDefinition interface and ResourceRegistry
- Implement URI template matching with parameter extraction
- Create ResourceResolver service for fetching resource data
- Implement 5 resource types:
  - metabase://database/{id}
  - metabase://database/{id}/tables
  - metabase://dashboard/{id}
  - metabase://card/{id}
  - metabase://collection/{id}
- Update McpProtocolHandler for resources/list and resources/read

Part of #3
```

---

## Notes

- Resources are read-only by definition
- Use JSON as the standard format for resource content
- Cache frequently accessed resources (consider for Phase 4)
- Resource URIs should be stable and predictable
- Handle large resources gracefully (pagination for lists)
