package com.aptasystems.kakapo.exception;

public class PayloadTooLargeException extends ApiException {
    public PayloadTooLargeException() {
        super(AsyncResult.PayloadTooLarge);
    }
}
