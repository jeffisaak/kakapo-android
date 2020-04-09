package com.aptasystems.kakapo.exception;

public class ItemSerializationFailedException extends ApiException {
    public ItemSerializationFailedException(Throwable cause) {
        super(cause, AsyncResult.ItemSerializationFailed);
    }
}
