package com.aptasystems.kakapo.exception;

public class AccountEncryptionFailedException extends ApiException {
    public AccountEncryptionFailedException(Throwable cause) {
        super(cause, AsyncResult.AccountEncryptionFailed);
    }
}
