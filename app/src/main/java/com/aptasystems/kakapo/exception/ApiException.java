package com.aptasystems.kakapo.exception;

public class ApiException extends Exception {

    private AsyncResult _errorCode;

    public ApiException(AsyncResult errorCode) {
        super();
        _errorCode = errorCode;
    }

    public ApiException(Throwable source, AsyncResult errorCode) {
        super(source);
        _errorCode = errorCode;
    }

    public AsyncResult getErrorCode() {
        return _errorCode;
    }
}
