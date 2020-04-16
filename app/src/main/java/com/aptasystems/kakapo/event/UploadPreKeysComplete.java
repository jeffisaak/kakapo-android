package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class UploadPreKeysComplete {

    private AsyncResult _status;

    public static UploadPreKeysComplete success() {
        return new UploadPreKeysComplete();
    }

    public static UploadPreKeysComplete failure(AsyncResult status) {
        UploadPreKeysComplete result = new UploadPreKeysComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }
}
