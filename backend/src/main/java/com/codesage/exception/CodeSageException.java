package com.codesage.exception;

/**
 * Base exception for all CodeSage application exceptions.
 */
public class CodeSageException extends RuntimeException {

    public CodeSageException(String message) {
        super(message);
    }

    public CodeSageException(String message, Throwable cause) {
        super(message, cause);
    }
}
