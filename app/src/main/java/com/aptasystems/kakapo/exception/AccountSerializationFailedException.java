package com.aptasystems.kakapo.exception;

public class AccountSerializationFailedException extends ApiException {
    public AccountSerializationFailedException(Throwable cause) {
        super(cause, AsyncResult.AccountSerializationFailed);
    }
}
