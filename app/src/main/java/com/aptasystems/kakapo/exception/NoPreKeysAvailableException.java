package com.aptasystems.kakapo.exception;

public class NoPreKeysAvailableException extends ApiException {

    public NoPreKeysAvailableException(Throwable cause) {
        super(cause, AsyncResult.NoPreKeysAvailable);
    }

    public NoPreKeysAvailableException() {
        super(AsyncResult.NoPreKeysAvailable);
    }
}
