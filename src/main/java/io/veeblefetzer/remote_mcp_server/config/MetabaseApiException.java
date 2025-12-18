package io.veeblefetzer.remote_mcp_server.config;

/**
 * Exception thrown when a Metabase API call fails.
 * Contains the HTTP status code and response body for debugging purposes.
 */
public class MetabaseApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    /**
     * Creates a new MetabaseApiException.
     *
     * @param statusCode   the HTTP status code returned by Metabase
     * @param responseBody the response body containing error details
     */
    public MetabaseApiException(int statusCode, String responseBody) {
        super(String.format("Metabase API error: %d - %s", statusCode, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Creates a new MetabaseApiException with a custom message.
     *
     * @param message      custom error message
     * @param statusCode   the HTTP status code returned by Metabase
     * @param responseBody the response body containing error details
     */
    public MetabaseApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Creates a new MetabaseApiException with a cause.
     *
     * @param message      custom error message
     * @param cause        the underlying cause
     * @param statusCode   the HTTP status code returned by Metabase
     * @param responseBody the response body containing error details
     */
    public MetabaseApiException(String message, Throwable cause, int statusCode, String responseBody) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Gets the HTTP status code from the failed API call.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the response body from the failed API call.
     *
     * @return the response body as a string
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Checks if this is a client error (4xx status code).
     *
     * @return true if the status code is in the 4xx range
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Checks if this is a server error (5xx status code).
     *
     * @return true if the status code is in the 5xx range
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Checks if this is an authentication error (401 or 403).
     *
     * @return true if the status code is 401 or 403
     */
    public boolean isAuthenticationError() {
        return statusCode == 401 || statusCode == 403;
    }

    /**
     * Checks if this is a not found error (404).
     *
     * @return true if the status code is 404
     */
    public boolean isNotFoundError() {
        return statusCode == 404;
    }
}
