package com.aptasystems.kakapo.exception;

public class NotFoundException extends ApiException {
    public NotFoundException() {
        super(AsyncResult.NotFound);
    }
}
