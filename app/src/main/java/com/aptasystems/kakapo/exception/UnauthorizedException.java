package com.aptasystems.kakapo.exception;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException() {
        super(AsyncResult.Unauthorized);
    }
}
