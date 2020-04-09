package com.aptasystems.kakapo.exception;

public class KeyGenerationFailedException extends ApiException {
    public KeyGenerationFailedException(Throwable cause) {
        super(cause, AsyncResult.KeyGenerationFailed);
    }
}
