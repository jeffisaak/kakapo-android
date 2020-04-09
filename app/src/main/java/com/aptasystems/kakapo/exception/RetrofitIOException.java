package com.aptasystems.kakapo.exception;

public class RetrofitIOException extends ApiException {
    public RetrofitIOException() {
        super(AsyncResult.RetrofitIOException);
    }
}
