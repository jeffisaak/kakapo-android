package com.aptasystems.kakapo.exception;

public class EncryptionFailedException extends ApiException {
    public EncryptionFailedException(Throwable cause) {
        super(cause, AsyncResult.EncryptionFailed);
    }
}
