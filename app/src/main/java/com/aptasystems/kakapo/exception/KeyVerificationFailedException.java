package com.aptasystems.kakapo.exception;

public class KeyVerificationFailedException extends ApiException {
    public KeyVerificationFailedException(Throwable cause) {
        super(cause, AsyncResult.KeyVerificationFailed);
    }
}
