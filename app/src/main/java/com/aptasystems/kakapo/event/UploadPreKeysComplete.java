package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.service.AccountBackupInfo;

public class UploadPreKeysComplete {

    private AsyncResult _status;

    public static UploadPreKeysComplete success() {
        UploadPreKeysComplete result = new UploadPreKeysComplete();
        return result;
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
