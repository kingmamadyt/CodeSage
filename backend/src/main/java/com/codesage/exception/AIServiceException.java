package com.codesage.exception;

/**
 * Exception thrown when AI service operations fail.
 */
public class AIServiceException extends CodeSageException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
