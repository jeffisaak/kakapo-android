package com.aptasystems.kakapo.exception;

public class ShareEncryptionFailedException extends ApiException {
    public ShareEncryptionFailedException(Throwable cause) {
        super(cause, AsyncResult.ShareEncryptionFailed);
    }
}
