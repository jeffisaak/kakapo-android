package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class AttachmentDecryptComplete {

    private AsyncResult _status;
    private byte[] _decryptedContent;

    public static AttachmentDecryptComplete success(byte[] decryptedData) {
        AttachmentDecryptComplete result = new AttachmentDecryptComplete();
        result.setStatus(AsyncResult.Success);
        result.setDecryptedContent(decryptedData);
        return result;
    }

    public static AttachmentDecryptComplete failure(AsyncResult status) {
        AttachmentDecryptComplete result = new AttachmentDecryptComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public byte[] getDecryptedContent() {
        return _decryptedContent;
    }

    public void setDecryptedContent(byte[] decryptedContent) {
        _decryptedContent = decryptedContent;
    }
}
