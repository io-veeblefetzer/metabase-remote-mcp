package io.veeblefetzer.remote_mcp_server.integration;

import io.veeblefetzer.metabase.api.MetabaseApi;
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
 * These tests are automatically skipped if the METABASE_API_KEY environment variable is not set.
 * 
 * To run these tests:
 * 1. Start Metabase: docker-compose up -d
 * 2. Set environment variables:
 *    export METABASE_URL=http://localhost:3000
 *    export METABASE_API_KEY=your-actual-api-key
 * 3. Run: mvn test -Dtest=MetabaseApiIntegrationTest
 */
@SpringBootTest
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "METABASE_API_KEY", matches = ".+")
class MetabaseApiIntegrationTest {

    @Autowired
    private ApiClient apiClient;

    private MetabaseApi metabaseApi;

    @BeforeEach
    void setUp() {
        metabaseApi = new MetabaseApi(apiClient);
    }

    @Test
    void canCallUserCurrentEndpoint() {
        // Test that we can authenticate and call the current user endpoint
        // Note: Due to OpenAPI spec complexity, the response type is simplified to Void
        // The test verifies that the API call doesn't throw an exception
        assertDoesNotThrow(() -> metabaseApi.apiUserCurrentGet().block());
    }

    @Test
    void canCallDatabaseListEndpoint() {
        // Test that we can list databases
        // Note: Due to OpenAPI spec complexity, the response type is simplified to Void
        // The test verifies that the API call doesn't throw an exception
        assertDoesNotThrow(() -> metabaseApi.apiDatabaseGet(null, null, null, null, null, null, null).block());
    }

    @Test
    void canCallDatabaseEndpointWithId() {
        // Test that we can get database details
        // Note: Due to OpenAPI spec complexity, the response type is simplified to Void
        // The test verifies that the API call doesn't throw an exception for a valid database ID
        // The sample database typically has ID 1
        assertDoesNotThrow(() -> metabaseApi.apiDatabaseIdGet(1, null, null, null).block());
    }
}
