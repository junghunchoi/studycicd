package junghun.studycicd.simulation;

import org.springframework.http.HttpStatus;

import java.util.Random;

public enum ErrorType {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, new String[]{
            "Unexpected internal server error occurred",
            "Failed to process request due to server error",
            "Database query execution failed",
            "Service initialization error",
            "Critical system component unavailable"
    }),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, new String[]{
            "Invalid request parameters provided",
            "Missing required fields in request body",
            "Request payload validation failed",
            "Malformed JSON in request",
            "Invalid data format detected"
    }),

    NOT_FOUND(HttpStatus.NOT_FOUND, new String[]{
            "Requested resource not found",
            "User ID does not exist in database",
            "Entity not found for given identifier",
            "Requested endpoint is not available",
            "Resource has been deleted or moved"
    }),

    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, new String[]{
            "Service temporarily unavailable due to maintenance",
            "External dependency service is down",
            "Database connection pool exhausted",
            "System is under heavy load, please retry later",
            "Service circuit breaker is open"
    }),

    TIMEOUT(HttpStatus.REQUEST_TIMEOUT, new String[]{
            "Request processing timeout exceeded",
            "Database query timeout after 30 seconds",
            "External API call timed out",
            "Operation exceeded maximum allowed time",
            "Gateway timeout while waiting for response"
    }),

    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, new String[]{
            "Database connection failed: Connection refused",
            "SQL execution error: Deadlock detected",
            "Database transaction rollback occurred",
            "Database connection pool is full",
            "Failed to acquire database lock"
    }),

    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, new String[]{
            "Payment gateway API returned error",
            "Third-party authentication service unavailable",
            "External API rate limit exceeded",
            "Invalid response from upstream service",
            "Failed to connect to external service"
    }),

    MEMORY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, new String[]{
            "Out of memory error: Heap space exhausted",
            "Failed to allocate memory for operation",
            "Memory threshold exceeded, rejecting request",
            "GC overhead limit exceeded",
            "Unable to create native thread due to memory"
    });

    private final HttpStatus status;
    private final String[] messages;
    private static final Random random = new Random();

    ErrorType(HttpStatus status, String[] messages) {
        this.status = status;
        this.messages = messages;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getRandomMessage() {
        return messages[random.nextInt(messages.length)];
    }

    public String[] getAllMessages() {
        return messages;
    }

    public static ErrorType getRandomErrorType() {
        ErrorType[] types = values();
        return types[random.nextInt(types.length)];
    }
}
