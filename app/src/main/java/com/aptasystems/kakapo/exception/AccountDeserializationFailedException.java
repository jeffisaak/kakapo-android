package com.aptasystems.kakapo.exception;

public class AccountDeserializationFailedException extends ApiException {
    public AccountDeserializationFailedException(Throwable cause) {
        super(cause, AsyncResult.AccountDeserializationFailed);
    }
}
