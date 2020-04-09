package com.aptasystems.kakapo.exception;

public class QuotaExceededException extends ApiException {
    public QuotaExceededException() {
        super(AsyncResult.QuotaExceeded);
    }
}
