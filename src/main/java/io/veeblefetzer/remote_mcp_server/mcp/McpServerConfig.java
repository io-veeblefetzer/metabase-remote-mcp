package io.veeblefetzer.remote_mcp_server.mcp;

import io.veeblefetzer.remote_mcp_server.tools.MetabaseToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the MCP Server.
 * Registers all Metabase tools with the Spring AI MCP framework.
 */
@Configuration
public class McpServerConfig {

    /**
     * Provides tool callbacks to the MCP server.
     * This allows the MCP server to discover and execute our tools.
     *
     * @param metabaseToolService the service containing all Metabase tools
     * @return ToolCallbackProvider for MCP integration
     */
    @Bean
    public ToolCallbackProvider metabaseToolCallbackProvider(MetabaseToolService metabaseToolService) {
        return metabaseToolService;
    }
}
