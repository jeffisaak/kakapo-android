package com.aptasystems.kakapo.exception;

public class DecryptionFailedException extends ApiException {
    public DecryptionFailedException(Throwable cause) {
        super(cause, AsyncResult.DecryptionFailed);
    }
}
