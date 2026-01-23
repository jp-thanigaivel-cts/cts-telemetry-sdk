package com.cts.telemetry.api.exception;

/**
 * Custom exception thrown when a required dependency is missing for enabled
 * instrumentation.
 * This exception includes an error code for programmatic handling and a
 * descriptive message.
 */
public class OptelDependencyException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new OptelDependencyException with the specified error code and
     * message.
     *
     * @param errorCode the error code identifying the specific dependency issue
     * @param message   the detailed error message
     */
    public OptelDependencyException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new OptelDependencyException with the specified error code,
     * message, and cause.
     *
     * @param errorCode the error code identifying the specific dependency issue
     * @param message   the detailed error message
     * @param cause     the cause of this exception
     */
    public OptelDependencyException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("OptelDependencyException [errorCode=%s, message=%s]", errorCode, getMessage());
    }
}
