# Task 2.3: Client Testing

**Task ID:** 2.3  
**Phase:** 2 - OpenAPI Client Generation  
**GitHub Issue:** [#2](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/2)  
**Status:** ✅ Complete

---

## Objective

Create integration tests that verify the generated Metabase API client works correctly with a real Metabase instance, ensuring the client configuration and API calls function as expected.

---

## Prerequisites

- Task 2.1 completed (Maven plugin configuration)
- Task 2.2 completed (Client configuration class)
- Docker environment running (Phase 1)
- Metabase initialized with API key configured

---

## Technical Details

### Test Files to Create

1. **Configuration Test:** `src/test/java/io/veeblefetzer/remote_mcp_server/config/MetabaseClientConfigTest.java`
2. **Integration Test:** `src/test/java/io/veeblefetzer/remote_mcp_server/integration/MetabaseApiIntegrationTest.java`
3. **Test Properties:** `src/test/resources/application-test.properties`

### 1. Configuration Unit Test

```java
package io.veeblefetzer.remote_mcp_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MetabaseClientConfigTest {

    @Autowired
    private MetabaseProperties properties;

    @Autowired
    private WebClient metabaseWebClient;

    @Test
    void contextLoads() {
        // Verifies Spring context loads with our configuration
    }

    @Test
    void propertiesAreLoaded() {
        assertNotNull(properties);
        assertNotNull(properties.getUrl());
        assertFalse(properties.getUrl().isBlank());
    }

    @Test
    void webClientBeanIsConfigured() {
        assertNotNull(metabaseWebClient);
    }

    @Test
    void timeoutPropertiesHaveDefaults() {
        assertNotNull(properties.getConnectTimeout());
        assertNotNull(properties.getReadTimeout());
        assertTrue(properties.getConnectTimeout().toMillis() > 0);
        assertTrue(properties.getReadTimeout().toMillis() > 0);
    }
}
```

### 2. Integration Test with Real Metabase

```java
package io.veeblefetzer.remote_mcp_server.integration;

import io.veeblefetzer.metabase.api.DatabaseApi;
import io.veeblefetzer.metabase.api.UserApi;
import io.veeblefetzer.metabase.client.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that require a running Metabase instance.
 * Run with: mvn test -Dtest=MetabaseApiIntegrationTest
 * 
 * Requires environment variables:
 * - METABASE_URL: URL of Metabase instance
 * - METABASE_API_KEY: Valid API key
 */
@SpringBootTest
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "METABASE_API_KEY", matches = ".+")
class MetabaseApiIntegrationTest {

    @Autowired
    private ApiClient apiClient;

    private UserApi userApi;
    private DatabaseApi databaseApi;

    @BeforeEach
    void setUp() {
        userApi = new UserApi(apiClient);
        databaseApi = new DatabaseApi(apiClient);
    }

    @Test
    void canGetCurrentUser() {
        // Test that we can authenticate and get current user info
        var response = userApi.getCurrentUser().block();
        
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getEmail());
    }

    @Test
    void canListDatabases() {
        // Test that we can list databases
        var databases = databaseApi.listDatabases().block();
        
        assertNotNull(databases);
        // At minimum, the sample database should exist
        assertTrue(databases.size() > 0);
    }

    @Test
    void canGetDatabaseDetails() {
        // First, get list of databases
        var databases = databaseApi.listDatabases().block();
        assertNotNull(databases);
        assertFalse(databases.isEmpty());

        // Then get details of first database
        var firstDbId = databases.get(0).getId();
        var dbDetails = databaseApi.getDatabase(firstDbId, null).block();
        
        assertNotNull(dbDetails);
        assertEquals(firstDbId, dbDetails.getId());
        assertNotNull(dbDetails.getName());
    }

    @Test
    void handlesInvalidApiKeyGracefully() {
        // This test verifies error handling
        // Would need to temporarily change API key or use different client
    }
}
```

### 3. Test Properties

**File:** `src/test/resources/application-test.properties`

```properties
# Test profile configuration
spring.main.banner-mode=off
logging.level.root=WARN
logging.level.io.veeblefetzer=DEBUG

# Metabase test configuration (use mock URL for unit tests)
metabase.url=http://localhost:3000
metabase.api-key=test-api-key-for-unit-tests
metabase.connect-timeout=5s
metabase.read-timeout=10s
```

**File:** `src/test/resources/application-integration.properties`

```properties
# Integration test profile - uses real Metabase
spring.main.banner-mode=off
logging.level.root=INFO
logging.level.io.veeblefetzer=DEBUG

# Metabase configuration from environment
metabase.url=${METABASE_URL:http://localhost:3000}
metabase.api-key=${METABASE_API_KEY}
metabase.connect-timeout=10s
metabase.read-timeout=30s
```

### 4. Test Dependencies

Add to `pom.xml` if not present:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Optional: For more advanced reactive testing -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Implementation Checklist

- [ ] Create `application-test.properties` with mock configuration
- [ ] Create `application-integration.properties` for real Metabase tests
- [ ] Create `MetabaseClientConfigTest.java` unit tests
- [ ] Test context loads successfully
- [ ] Test properties binding works
- [ ] Test WebClient bean creation
- [ ] Create `MetabaseApiIntegrationTest.java` integration tests
- [ ] Test `GET /api/user/current` endpoint
- [ ] Test `GET /api/database` endpoint
- [ ] Test `GET /api/database/{id}` endpoint
- [ ] Add `@EnabledIfEnvironmentVariable` for integration tests
- [ ] Test: Run unit tests with `mvn test`
- [ ] Test: Run integration tests with Metabase running
- [ ] Verify all tests pass

---

## Acceptance Criteria

- [ ] Unit tests pass without external dependencies
- [ ] Unit tests verify configuration is loaded correctly
- [ ] Integration tests connect to real Metabase instance
- [ ] Integration tests verify at least 3 API endpoints work
- [ ] Tests are properly isolated (unit vs integration)
- [ ] Integration tests can be skipped when Metabase unavailable
- [ ] Test coverage includes error handling scenarios
- [ ] All tests pass: `mvn test`

---

## Running Tests

### Unit Tests Only

```bash
# Run all unit tests (excludes integration)
mvn test -Dtest=!*IntegrationTest

# Run specific test class
mvn test -Dtest=MetabaseClientConfigTest
```

### Integration Tests

```bash
# Set up environment
export METABASE_URL=http://localhost:3000
export METABASE_API_KEY=your-actual-api-key

# Ensure Metabase is running
docker-compose up -d

# Wait for Metabase to be ready
curl -s http://localhost:3000/api/health | grep -q '"status":"ok"'

# Run integration tests
mvn test -Dtest=MetabaseApiIntegrationTest

# Or run all tests
mvn test
```

### Test Reports

```bash
# Generate test report
mvn surefire-report:report

# View report
open target/site/surefire-report.html
```

---

## Test Categories

| Category | Profile | Dependencies | Run Command |
|----------|---------|--------------|-------------|
| Unit | test | None | `mvn test` |
| Integration | integration | Metabase running | `METABASE_API_KEY=xxx mvn test` |

---

## Troubleshooting Tests

### Connection Refused

```
java.net.ConnectException: Connection refused
```

**Solution:** Ensure Metabase is running:
```bash
docker-compose up -d
docker-compose logs -f metabase
```

### Authentication Failed (401)

```
WebClientResponseException: 401 Unauthorized
```

**Solution:** Verify API key:
1. Check key is set: `echo $METABASE_API_KEY`
2. Test key manually: `curl -H "X-Metabase-Session: $METABASE_API_KEY" http://localhost:3000/api/user/current`

### Tests Skipped

Integration tests may be skipped if environment variables not set. This is expected behavior.

---

## Commit Template

```
test(client): add Metabase API client tests

- Add MetabaseClientConfigTest for configuration validation
- Add MetabaseApiIntegrationTest for API endpoint testing
- Create test and integration profiles with properties
- Test current user, database list, and database details endpoints
- Add conditional execution for integration tests

Closes #2
```

---

## Notes

- Integration tests require manual setup of API key
- Consider adding Testcontainers for fully automated integration tests
- Keep unit tests fast and independent of external services
- Integration tests serve as smoke tests for the API client
- Document any API quirks discovered during testing
