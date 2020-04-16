package com.aptasystems.kakapo.exception;

public class InvalidKeyLengthException extends ApiException {
    public InvalidKeyLengthException() {
        super(AsyncResult.InvalidKeyLength);
    }
}
