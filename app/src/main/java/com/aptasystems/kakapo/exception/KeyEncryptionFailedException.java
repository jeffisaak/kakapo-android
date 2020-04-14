package com.aptasystems.kakapo.exception;

public class KeyEncryptionFailedException extends ApiException {
    public KeyEncryptionFailedException(Throwable cause) {
        super(cause, AsyncResult.KeyEncryptionFailed);
    }
}
