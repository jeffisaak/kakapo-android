package com.aptasystems.kakapo.exception;

public class ContentStreamFailedException extends ApiException {
    public ContentStreamFailedException(Throwable cause) {
        super(cause, AsyncResult.ContentStreamFailed);
    }
}
