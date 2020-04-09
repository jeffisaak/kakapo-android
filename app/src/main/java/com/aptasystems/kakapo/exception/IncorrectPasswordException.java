package com.aptasystems.kakapo.exception;

public class IncorrectPasswordException extends ApiException {
    public IncorrectPasswordException(Throwable cause) {
        super(cause, AsyncResult.IncorrectPassword);
    }
}
