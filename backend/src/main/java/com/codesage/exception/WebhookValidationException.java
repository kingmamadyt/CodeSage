package com.codesage.exception;

/**
 * Exception thrown when webhook validation fails.
 */
public class WebhookValidationException extends CodeSageException {

    public WebhookValidationException(String message) {
        super(message);
    }

    public WebhookValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
