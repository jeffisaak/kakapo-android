package com.aptasystems.kakapo.exception;

public class PreKeyNotFoundException extends ApiException {

    public PreKeyNotFoundException(Throwable cause) {
        super(cause, AsyncResult.PreKeyNotFound);
    }

    public PreKeyNotFoundException() {
        super(AsyncResult.PreKeyNotFound);
    }
}
