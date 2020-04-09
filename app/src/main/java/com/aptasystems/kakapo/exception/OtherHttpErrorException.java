package com.aptasystems.kakapo.exception;

public class OtherHttpErrorException extends ApiException {
    public OtherHttpErrorException() {
        super(AsyncResult.OtherHttpError);
    }
}
