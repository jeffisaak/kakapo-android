package com.aptasystems.kakapo.exception;

public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException() {
        super(AsyncResult.TooManyRequests);
    }
}
