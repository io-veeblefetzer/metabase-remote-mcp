# Task 3.1: Core MCP Infrastructure

**Task ID:** 3.1  
**Phase:** 3 - MCP Server Implementation  
**GitHub Issue:** [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)  
**Status:** ⬜ Not Started

---

## Objective

Implement the core MCP (Model Context Protocol) server infrastructure with SSE (Server-Sent Events) transport, enabling AI assistants to communicate with the server using the MCP protocol.

---

## Prerequisites

- Phase 2 completed (OpenAPI client generation)
- Understanding of MCP protocol specification
- Understanding of SSE (Server-Sent Events)
- Spring Boot WebFlux knowledge

---

## Technical Details

### MCP Protocol Overview

The Model Context Protocol uses JSON-RPC 2.0 over SSE for communication:

1. **Client connects** via SSE to `/sse` endpoint
2. **Client sends** tool call requests as JSON-RPC messages
3. **Server processes** requests and invokes appropriate tools
4. **Server responds** via SSE with results

### Files to Create

1. **SSE Controller:** `src/main/java/io/veeblefetzer/remote_mcp_server/controller/McpSseController.java`
2. **MCP Message Types:** `src/main/java/io/veeblefetzer/remote_mcp_server/mcp/model/`
3. **MCP Protocol Handler:** `src/main/java/io/veeblefetzer/remote_mcp_server/mcp/McpProtocolHandler.java`
4. **Health Controller:** `src/main/java/io/veeblefetzer/remote_mcp_server/controller/HealthController.java`

### 1. SSE Controller

```java
package io.veeblefetzer.remote_mcp_server.controller;

import io.veeblefetzer.remote_mcp_server.mcp.McpProtocolHandler;
import io.veeblefetzer.remote_mcp_server.mcp.model.JsonRpcRequest;
import io.veeblefetzer.remote_mcp_server.mcp.model.JsonRpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/mcp")
public class McpSseController {

    private static final Logger log = LoggerFactory.getLogger(McpSseController.class);
    
    private final McpProtocolHandler protocolHandler;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sessions;

    public McpSseController(McpProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
        this.sessions = new ConcurrentHashMap<>();
    }

    /**
     * SSE endpoint for MCP communication.
     * Clients connect here to receive server-sent events.
     */
    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> connect() {
        String sessionId = UUID.randomUUID().toString();
        log.info("New MCP session: {}", sessionId);

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessions.put(sessionId, sink);

        // Send session ID as first event
        sink.tryEmitNext(ServerSentEvent.<String>builder()
            .event("session")
            .data(sessionId)
            .build());

        // Send periodic heartbeat to keep connection alive
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
            .map(i -> ServerSentEvent.<String>builder()
                .event("heartbeat")
                .data("{\"type\":\"ping\"}")
                .build());

        return Flux.merge(sink.asFlux(), heartbeat)
            .doOnCancel(() -> {
                log.info("MCP session closed: {}", sessionId);
                sessions.remove(sessionId);
            })
            .doOnError(e -> {
                log.error("MCP session error: {}", sessionId, e);
                sessions.remove(sessionId);
            });
    }

    /**
     * Endpoint to receive JSON-RPC requests from clients.
     * Responses are sent back via the SSE stream.
     */
    @PostMapping(path = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonRpcResponse> handleMessage(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody JsonRpcRequest request) {
        
        log.debug("Received request for session {}: {}", sessionId, request.getMethod());
        
        return protocolHandler.handleRequest(request)
            .doOnNext(response -> {
                // Also send via SSE if session exists
                Sinks.Many<ServerSentEvent<String>> sink = sessions.get(sessionId);
                if (sink != null) {
                    // Serialize and send response
                }
            });
    }
}
```

### 2. MCP Message Types

**JsonRpcRequest.java:**
```java
package io.veeblefetzer.remote_mcp_server.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRpcRequest {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("id")
    private Object id;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private Object params;
    
    // Getters and setters...
}
```

**JsonRpcResponse.java:**
```java
package io.veeblefetzer.remote_mcp_server.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("id")
    private Object id;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private JsonRpcError error;
    
    // Static factory methods
    public static JsonRpcResponse success(Object id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }
    
    public static JsonRpcResponse error(Object id, int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setError(new JsonRpcError(code, message));
        return response;
    }
    
    // Getters and setters...
}
```

**JsonRpcError.java:**
```java
package io.veeblefetzer.remote_mcp_server.mcp.model;

public class JsonRpcError {
    
    private int code;
    private String message;
    private Object data;
    
    // Standard JSON-RPC error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // Constructor, getters, setters...
}
```

### 3. MCP Protocol Handler

```java
package io.veeblefetzer.remote_mcp_server.mcp;

import io.veeblefetzer.remote_mcp_server.mcp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class McpProtocolHandler {
    
    private static final Logger log = LoggerFactory.getLogger(McpProtocolHandler.class);
    
    // MCP method names
    private static final String METHOD_INITIALIZE = "initialize";
    private static final String METHOD_LIST_TOOLS = "tools/list";
    private static final String METHOD_CALL_TOOL = "tools/call";
    private static final String METHOD_LIST_RESOURCES = "resources/list";
    private static final String METHOD_READ_RESOURCE = "resources/read";
    
    public Mono<JsonRpcResponse> handleRequest(JsonRpcRequest request) {
        log.debug("Handling MCP method: {}", request.getMethod());
        
        return switch (request.getMethod()) {
            case METHOD_INITIALIZE -> handleInitialize(request);
            case METHOD_LIST_TOOLS -> handleListTools(request);
            case METHOD_CALL_TOOL -> handleCallTool(request);
            case METHOD_LIST_RESOURCES -> handleListResources(request);
            case METHOD_READ_RESOURCE -> handleReadResource(request);
            default -> Mono.just(JsonRpcResponse.error(
                request.getId(),
                JsonRpcError.METHOD_NOT_FOUND,
                "Method not found: " + request.getMethod()
            ));
        };
    }
    
    private Mono<JsonRpcResponse> handleInitialize(JsonRpcRequest request) {
        Map<String, Object> result = Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of(),
                "resources", Map.of()
            ),
            "serverInfo", Map.of(
                "name", "metabase-mcp-server",
                "version", "1.0.0"
            )
        );
        return Mono.just(JsonRpcResponse.success(request.getId(), result));
    }
    
    private Mono<JsonRpcResponse> handleListTools(JsonRpcRequest request) {
        // Will be implemented in Task 3.2
        return Mono.just(JsonRpcResponse.success(request.getId(), Map.of("tools", List.of())));
    }
    
    private Mono<JsonRpcResponse> handleCallTool(JsonRpcRequest request) {
        // Will be implemented in Task 3.3
        return Mono.just(JsonRpcResponse.error(
            request.getId(),
            JsonRpcError.METHOD_NOT_FOUND,
            "Tool execution not yet implemented"
        ));
    }
    
    private Mono<JsonRpcResponse> handleListResources(JsonRpcRequest request) {
        // Will be implemented in Task 3.4
        return Mono.just(JsonRpcResponse.success(request.getId(), Map.of("resources", List.of())));
    }
    
    private Mono<JsonRpcResponse> handleReadResource(JsonRpcRequest request) {
        // Will be implemented in Task 3.4
        return Mono.just(JsonRpcResponse.error(
            request.getId(),
            JsonRpcError.METHOD_NOT_FOUND,
            "Resource reading not yet implemented"
        ));
    }
}
```

### 4. Health Controller

```java
package io.veeblefetzer.remote_mcp_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of(
            "status", "ok",
            "service", "metabase-mcp-server"
        ));
    }
}
```

---

## Implementation Checklist

- [ ] Create `mcp.model` package with JSON-RPC classes
- [ ] Create `JsonRpcRequest.java` with proper Jackson annotations
- [ ] Create `JsonRpcResponse.java` with factory methods
- [ ] Create `JsonRpcError.java` with standard error codes
- [ ] Create `McpProtocolHandler.java` service
- [ ] Implement `initialize` method handler
- [ ] Add stub handlers for `tools/list`, `tools/call`
- [ ] Add stub handlers for `resources/list`, `resources/read`
- [ ] Create `McpSseController.java` with SSE endpoint
- [ ] Implement session management
- [ ] Add heartbeat mechanism
- [ ] Create `HealthController.java`
- [ ] Test: Application starts successfully
- [ ] Test: SSE endpoint accepts connections
- [ ] Test: Initialize method returns server info
- [ ] Test: Health endpoint responds

---

## Acceptance Criteria

- [ ] SSE endpoint available at `/mcp/sse`
- [ ] Health endpoint available at `/health`
- [ ] MCP `initialize` method returns proper server info
- [ ] MCP `tools/list` returns empty list (stub)
- [ ] MCP `resources/list` returns empty list (stub)
- [ ] Unknown methods return proper error response
- [ ] SSE connections receive heartbeat events
- [ ] Session management tracks active connections
- [ ] All code compiles without errors

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/mcp/sse` | SSE connection endpoint |
| POST | `/mcp/message` | JSON-RPC message endpoint |
| GET | `/health` | Health check |

---

## Testing the SSE Endpoint

```bash
# Connect to SSE endpoint
curl -N http://localhost:8080/mcp/sse

# Expected output:
# event:session
# data:abc123-session-id
#
# event:heartbeat
# data:{"type":"ping"}

# Send initialize request
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: abc123-session-id" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }'

# Expected response:
# {
#   "jsonrpc": "2.0",
#   "id": 1,
#   "result": {
#     "protocolVersion": "2024-11-05",
#     "capabilities": {...},
#     "serverInfo": {...}
#   }
# }
```

---

## MCP Protocol Reference

### Standard Methods

| Method | Description |
|--------|-------------|
| `initialize` | Initialize connection, exchange capabilities |
| `tools/list` | List available tools |
| `tools/call` | Execute a tool |
| `resources/list` | List available resources |
| `resources/read` | Read a resource |

### JSON-RPC Error Codes

| Code | Meaning |
|------|---------|
| -32700 | Parse error |
| -32600 | Invalid request |
| -32601 | Method not found |
| -32602 | Invalid params |
| -32603 | Internal error |

---

## Commit Template

```
feat(mcp): implement core MCP server infrastructure

- Create JSON-RPC message model classes
- Implement McpProtocolHandler with method routing
- Create McpSseController with SSE endpoint
- Add session management and heartbeat mechanism
- Implement initialize method handler
- Add health check endpoint
- Add stub handlers for tools and resources

Part of #3
```

---

## Notes

- SSE is used instead of WebSocket for better compatibility
- Heartbeat prevents connection timeout by proxies/load balancers
- Session management allows for future stateful operations
- JSON-RPC 2.0 is the standard protocol for MCP
- Protocol version should match MCP specification
