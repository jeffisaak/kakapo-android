package com.aptasystems.kakapo.exception;

public class ServerUnavailableException extends ApiException {
    public ServerUnavailableException() {
        super(AsyncResult.ServerUnavailable);
    }
}
