# Task 3.5: Testing Suite

**Task ID:** 3.5  
**Phase:** 3 - MCP Server Implementation  
**GitHub Issue:** [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)  
**Status:** ⬜ Not Started

---

## Objective

Create a comprehensive test suite covering unit tests, integration tests, and end-to-end tests for the MCP server implementation, ensuring reliability and correctness of all components.

---

## Prerequisites

- Task 3.1 completed (Core MCP infrastructure)
- Task 3.2 completed (Tool definitions)
- Task 3.3 completed (Tool executor)
- Task 3.4 completed (Resources)
- Testing frameworks configured in `pom.xml`

---

## Technical Details

### Test Structure

```
src/test/java/io/veeblefetzer/remote_mcp_server/
├── unit/
│   ├── mcp/
│   │   ├── McpProtocolHandlerTest.java
│   │   ├── JsonRpcRequestTest.java
│   │   └── JsonRpcResponseTest.java
│   ├── tools/
│   │   ├── ToolRegistryTest.java
│   │   └── ToolDefinitionTest.java
│   └── resources/
│       ├── ResourceRegistryTest.java
│       └── ResourceResolverTest.java
├── integration/
│   ├── McpSseControllerIT.java
│   ├── MetabaseToolExecutorIT.java
│   └── ResourceResolverIT.java
└── e2e/
    └── McpServerE2ETest.java
```

### Test Dependencies

Add to `pom.xml` if not present:

```xml
<dependencies>
    <!-- Test dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers for integration tests -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- WebTestClient for SSE testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Unit Tests

### 1. McpProtocolHandlerTest

```java
package io.veeblefetzer.remote_mcp_server.unit.mcp;

import io.veeblefetzer.remote_mcp_server.mcp.McpProtocolHandler;
import io.veeblefetzer.remote_mcp_server.mcp.model.*;
import io.veeblefetzer.remote_mcp_server.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpProtocolHandlerTest {

    @Mock
    private ToolRegistry toolRegistry;
    
    private McpProtocolHandler handler;
    
    @BeforeEach
    void setUp() {
        handler = new McpProtocolHandler(toolRegistry);
    }
    
    @Test
    void handleInitialize_returnsServerInfo() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setId(1);
        request.setMethod("initialize");
        request.setParams(Map.of(
            "protocolVersion", "2024-11-05",
            "clientInfo", Map.of("name", "test-client")
        ));
        
        StepVerifier.create(handler.handleRequest(request))
            .assertNext(response -> {
                assertNotNull(response.getResult());
                Map<String, Object> result = (Map<String, Object>) response.getResult();
                assertEquals("2024-11-05", result.get("protocolVersion"));
                assertNotNull(result.get("serverInfo"));
            })
            .verifyComplete();
    }
    
    @Test
    void handleUnknownMethod_returnsError() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setId(1);
        request.setMethod("unknown/method");
        
        StepVerifier.create(handler.handleRequest(request))
            .assertNext(response -> {
                assertNotNull(response.getError());
                assertEquals(JsonRpcError.METHOD_NOT_FOUND, response.getError().getCode());
            })
            .verifyComplete();
    }
    
    @Test
    void handleListTools_returnsToolList() {
        when(toolRegistry.listTools()).thenReturn(List.of(
            Map.of("name", "test-tool", "description", "A test tool")
        ));
        
        JsonRpcRequest request = new JsonRpcRequest();
        request.setId(1);
        request.setMethod("tools/list");
        
        StepVerifier.create(handler.handleRequest(request))
            .assertNext(response -> {
                Map<String, Object> result = (Map<String, Object>) response.getResult();
                List<?> tools = (List<?>) result.get("tools");
                assertEquals(1, tools.size());
            })
            .verifyComplete();
    }
}
```

### 2. ToolRegistryTest

```java
package io.veeblefetzer.remote_mcp_server.unit.tools;

import io.veeblefetzer.remote_mcp_server.tools.*;
import io.veeblefetzer.remote_mcp_server.tools.definitions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {
    
    private ToolRegistry registry;
    
    @BeforeEach
    void setUp() {
        List<ToolDefinition> tools = List.of(
            new ListDatabasesTool(),
            new GetDatabaseTool(),
            new ExecuteQueryTool()
        );
        registry = new ToolRegistry(tools);
    }
    
    @Test
    void listTools_returnsAllRegisteredTools() {
        var tools = registry.listTools();
        assertEquals(3, tools.size());
    }
    
    @Test
    void getTool_existingTool_returnsToolDefinition() {
        var tool = registry.getTool("list-databases");
        assertTrue(tool.isPresent());
        assertEquals("list-databases", tool.get().getName());
    }
    
    @Test
    void getTool_unknownTool_returnsEmpty() {
        var tool = registry.getTool("unknown-tool");
        assertFalse(tool.isPresent());
    }
    
    @Test
    void hasTool_existingTool_returnsTrue() {
        assertTrue(registry.hasTool("get-database"));
    }
    
    @Test
    void hasTool_unknownTool_returnsFalse() {
        assertFalse(registry.hasTool("nonexistent"));
    }
    
    @Test
    void toolDefinition_hasValidInputSchema() {
        var tool = registry.getTool("execute-query").orElseThrow();
        var schema = tool.getInputSchema();
        
        assertEquals("object", schema.get("type"));
        assertNotNull(schema.get("properties"));
        assertNotNull(schema.get("required"));
    }
}
```

### 3. ResourceRegistryTest

```java
package io.veeblefetzer.remote_mcp_server.unit.resources;

import io.veeblefetzer.remote_mcp_server.resources.*;
import io.veeblefetzer.remote_mcp_server.resources.definitions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceRegistryTest {
    
    private ResourceRegistry registry;
    
    @BeforeEach
    void setUp() {
        List<ResourceDefinition> resources = List.of(
            new DatabaseResource(),
            new DashboardResource(),
            new CardResource()
        );
        registry = new ResourceRegistry(resources);
    }
    
    @Test
    void matchUri_validDatabaseUri_returnsMatch() {
        var match = registry.matchUri("metabase://database/123");
        
        assertTrue(match.isPresent());
        assertEquals("123", match.get().getParams().get("id"));
    }
    
    @Test
    void matchUri_invalidUri_returnsEmpty() {
        var match = registry.matchUri("invalid://uri");
        assertFalse(match.isPresent());
    }
    
    @Test
    void listResources_returnsAllTemplates() {
        var resources = registry.listResources();
        assertEquals(3, resources.size());
    }
}
```

---

## Integration Tests

### 1. McpSseControllerIT

```java
package io.veeblefetzer.remote_mcp_server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class McpSseControllerIT {
    
    @Autowired
    private WebTestClient webClient;
    
    @Test
    void sseEndpoint_acceptsConnection() {
        webClient.get()
            .uri("/mcp/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }
    
    @Test
    void healthEndpoint_returnsOk() {
        webClient.get()
            .uri("/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok");
    }
    
    @Test
    void messageEndpoint_handlesInitialize() {
        String request = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05"
                }
            }
            """;
        
        webClient.post()
            .uri("/mcp/message")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Session-Id", "test-session")
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.result.protocolVersion").isEqualTo("2024-11-05")
            .jsonPath("$.result.serverInfo.name").exists();
    }
    
    @Test
    void messageEndpoint_handlesToolsList() {
        String request = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/list",
                "params": {}
            }
            """;
        
        webClient.post()
            .uri("/mcp/message")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Session-Id", "test-session")
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.result.tools").isArray()
            .jsonPath("$.result.tools.length()").isNumber();
    }
}
```

### 2. MetabaseToolExecutorIT (with Testcontainers)

```java
package io.veeblefetzer.remote_mcp_server.integration;

import io.veeblefetzer.remote_mcp_server.service.MetabaseToolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.util.Map;

/**
 * Integration tests for MetabaseToolExecutor.
 * Requires running Metabase instance.
 */
@SpringBootTest
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "METABASE_API_KEY", matches = ".+")
class MetabaseToolExecutorIT {
    
    @Autowired
    private MetabaseToolExecutor executor;
    
    @Test
    void listDatabases_returnsResults() {
        StepVerifier.create(executor.execute("list-databases", Map.of()))
            .assertNext(result -> {
                assertFalse(result.isError());
            })
            .verifyComplete();
    }
    
    @Test
    void getCurrentUser_returnsUserInfo() {
        StepVerifier.create(executor.execute("get-current-user", Map.of()))
            .assertNext(result -> {
                assertFalse(result.isError());
            })
            .verifyComplete();
    }
    
    @Test
    void executeQuery_withValidQuery_returnsResults() {
        Map<String, Object> params = Map.of(
            "database_id", 1,
            "query", "SELECT 1 as test_value"
        );
        
        StepVerifier.create(executor.execute("execute-query", params))
            .assertNext(result -> {
                assertFalse(result.isError());
            })
            .verifyComplete();
    }
    
    @Test
    void getDatabase_withMissingId_returnsError() {
        StepVerifier.create(executor.execute("get-database", Map.of()))
            .assertNext(result -> {
                assertTrue(result.isError());
            })
            .verifyComplete();
    }
}
```

---

## End-to-End Tests

### McpServerE2ETest

```java
package io.veeblefetzer.remote_mcp_server.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * End-to-end tests simulating MCP client behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "METABASE_API_KEY", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpServerE2ETest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private WebClient client;
    private String sessionId;
    
    @BeforeEach
    void setUp() {
        client = WebClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }
    
    @Test
    @Order(1)
    void completeWorkflow_initializeAndQueryData() throws Exception {
        // 1. Initialize session
        var initResponse = sendRequest("initialize", Map.of(
            "protocolVersion", "2024-11-05",
            "clientInfo", Map.of("name", "e2e-test")
        ));
        assertNotNull(initResponse.get("result"));
        
        // 2. List available tools
        var toolsResponse = sendRequest("tools/list", Map.of());
        var tools = (List<?>) ((Map<?, ?>) toolsResponse.get("result")).get("tools");
        assertTrue(tools.size() >= 10);
        
        // 3. List databases
        var dbResponse = sendRequest("tools/call", Map.of(
            "name", "list-databases",
            "arguments", Map.of()
        ));
        assertFalse((Boolean) ((Map<?, ?>) dbResponse.get("result")).get("isError"));
        
        // 4. List resources
        var resourcesResponse = sendRequest("resources/list", Map.of());
        var resources = (List<?>) ((Map<?, ?>) resourcesResponse.get("result")).get("resources");
        assertTrue(resources.size() >= 5);
    }
    
    private Map<String, Object> sendRequest(String method, Map<String, Object> params) {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "jsonrpc", "2.0",
            "id", System.currentTimeMillis(),
            "method", method,
            "params", params
        ));
        
        return client.post()
            .uri("/mcp/message")
            .header("Content-Type", "application/json")
            .header("X-Session-Id", "e2e-test-session")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
```

---

## Test Configuration Files

### application-test.properties

```properties
# Test profile - mocks external services
spring.main.banner-mode=off
logging.level.root=WARN
logging.level.io.veeblefetzer=DEBUG

# Mock Metabase configuration
metabase.url=http://localhost:3000
metabase.api-key=test-key
metabase.connect-timeout=5s
metabase.read-timeout=10s
```

### application-integration.properties

```properties
# Integration test profile - uses real Metabase
spring.main.banner-mode=off
logging.level.root=INFO
logging.level.io.veeblefetzer=DEBUG

metabase.url=${METABASE_URL:http://localhost:3000}
metabase.api-key=${METABASE_API_KEY}
metabase.connect-timeout=10s
metabase.read-timeout=30s
```

---

## Implementation Checklist

### Unit Tests
- [ ] Create `unit` package structure
- [ ] Write `McpProtocolHandlerTest` (5+ tests)
- [ ] Write `JsonRpcRequestTest` (3+ tests)
- [ ] Write `JsonRpcResponseTest` (3+ tests)
- [ ] Write `ToolRegistryTest` (5+ tests)
- [ ] Write `ToolDefinitionTest` for each tool
- [ ] Write `ResourceRegistryTest` (5+ tests)
- [ ] Achieve 80%+ unit test coverage

### Integration Tests
- [ ] Create `integration` package
- [ ] Write `McpSseControllerIT` (5+ tests)
- [ ] Write `MetabaseToolExecutorIT` (10+ tests)
- [ ] Write `ResourceResolverIT` (5+ tests)
- [ ] Configure test profiles

### End-to-End Tests
- [ ] Create `e2e` package
- [ ] Write `McpServerE2ETest` with full workflow
- [ ] Test SSE connection lifecycle
- [ ] Test complete tool execution workflow
- [ ] Test resource resolution workflow

### Test Infrastructure
- [ ] Add test dependencies to `pom.xml`
- [ ] Create `application-test.properties`
- [ ] Create `application-integration.properties`
- [ ] Configure test environment variables

---

## Acceptance Criteria

- [ ] Minimum 30 unit tests
- [ ] Minimum 10 integration tests
- [ ] Minimum 3 end-to-end tests
- [ ] 80%+ code coverage for business logic
- [ ] All tests pass: `mvn test`
- [ ] Integration tests pass with real Metabase
- [ ] Tests are properly categorized and isolated

---

## Running Tests

```bash
# Run all tests
mvn test

# Run unit tests only
mvn test -Dtest="**/unit/**"

# Run integration tests (requires Metabase)
export METABASE_API_KEY=your-api-key
mvn test -Dtest="**/integration/**"

# Run E2E tests
export METABASE_API_KEY=your-api-key
mvn test -Dtest="**/e2e/**"

# Generate coverage report
mvn test jacoco:report
open target/site/jacoco/index.html
```

---

## Commit Template

```
test(mcp): add comprehensive test suite

Unit Tests:
- McpProtocolHandlerTest: MCP protocol handling
- ToolRegistryTest: Tool registration and lookup
- ResourceRegistryTest: Resource template matching
- JSON-RPC request/response serialization tests

Integration Tests:
- McpSseControllerIT: SSE endpoint testing
- MetabaseToolExecutorIT: Tool execution with real API
- ResourceResolverIT: Resource resolution testing

End-to-End Tests:
- McpServerE2ETest: Complete workflow simulation

Test Infrastructure:
- Test profiles for different environments
- Testcontainers configuration for isolation
- Coverage reporting with JaCoCo

Closes #3
```

---

## Notes

- Use `@EnabledIfEnvironmentVariable` for tests requiring Metabase
- Mock external dependencies in unit tests
- Integration tests may be slower - run selectively during development
- Consider adding Testcontainers for fully isolated Metabase testing
- Keep test data minimal but meaningful
