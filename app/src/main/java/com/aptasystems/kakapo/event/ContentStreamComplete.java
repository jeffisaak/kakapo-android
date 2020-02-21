package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class ContentStreamComplete {

    private AsyncResult _status;
    private byte[] _encryptedContent;

    public static ContentStreamComplete success(byte[] encryptedContent) {
        ContentStreamComplete result = new ContentStreamComplete();
        result.setStatus(AsyncResult.Success);
        result.setEncryptedContent(encryptedContent);
        return result;
    }

    public static ContentStreamComplete failure(AsyncResult status) {
        ContentStreamComplete result = new ContentStreamComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public byte[] getEncryptedContent() {
        return _encryptedContent;
    }

    public void setEncryptedContent(byte[] encryptedContent) {
        _encryptedContent = encryptedContent;
    }
}
