package com.aptasystems.kakapo.exception;

// TODO: Give this a better name?
public class ConflictException extends ApiException {
    public ConflictException() {
        super(AsyncResult.Conflict);
    }
}
