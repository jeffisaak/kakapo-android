package com.aptasystems.kakapo.exception;

public class BadRequestException extends ApiException {
    public BadRequestException() {
        super(AsyncResult.BadRequest);
    }
}
