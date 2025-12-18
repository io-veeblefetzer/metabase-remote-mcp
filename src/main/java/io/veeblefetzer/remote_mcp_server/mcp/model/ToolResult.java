package io.veeblefetzer.remote_mcp_server.mcp.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of an MCP tool execution.
 * Contains either successful content or an error message.
 */
public class ToolResult {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final boolean isError;
    private final List<ContentBlock> content;

    private ToolResult(boolean isError, List<ContentBlock> content) {
        this.isError = isError;
        this.content = content;
    }

    /**
     * Creates a successful tool result with the given data.
     *
     * @param data the result data to serialize as JSON
     * @return a successful ToolResult
     */
    public static ToolResult success(Object data) {
        return new ToolResult(false, List.of(ContentBlock.text(serializeToJson(data))));
    }

    /**
     * Creates a successful tool result with raw text content.
     *
     * @param text the text content
     * @return a successful ToolResult
     */
    public static ToolResult successText(String text) {
        return new ToolResult(false, List.of(ContentBlock.text(text)));
    }

    /**
     * Creates an error tool result with the given message.
     *
     * @param message the error message
     * @return an error ToolResult
     */
    public static ToolResult error(String message) {
        return new ToolResult(true, List.of(ContentBlock.text(message)));
    }

    /**
     * Converts this result to a Map suitable for MCP protocol.
     *
     * @return Map representation of this result
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "isError", isError,
                "content", content.stream()
                        .map(ContentBlock::toMap)
                        .toList()
        );
    }

    public boolean isError() {
        return isError;
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    /**
     * Gets the text content of this result (first content block).
     *
     * @return the text content or empty string if no content
     */
    public String getTextContent() {
        if (content.isEmpty()) {
            return "";
        }
        return content.get(0).getText();
    }

    private static String serializeToJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * Represents a content block in the MCP response.
     */
    public static class ContentBlock {
        private final String type;
        private final String text;

        private ContentBlock(String type, String text) {
            this.type = type;
            this.text = text;
        }

        /**
         * Creates a text content block.
         *
         * @param text the text content
         * @return a text ContentBlock
         */
        public static ContentBlock text(String text) {
            return new ContentBlock("text", text);
        }

        public Map<String, Object> toMap() {
            return Map.of("type", type, "text", text);
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }
}
