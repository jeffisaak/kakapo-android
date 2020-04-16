package com.aptasystems.kakapo.exception;

public class ItemDeserializationFailedException extends ApiException {
    public ItemDeserializationFailedException(Throwable cause) {
        super(cause, AsyncResult.ItemDeserializationFailed);
    }
}
