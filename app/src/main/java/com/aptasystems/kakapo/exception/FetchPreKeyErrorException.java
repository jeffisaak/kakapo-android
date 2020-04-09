package com.aptasystems.kakapo.exception;

public class FetchPreKeyErrorException extends ApiException {
    public FetchPreKeyErrorException() {
        super(AsyncResult.FetchPreKeyError);
    }
}
