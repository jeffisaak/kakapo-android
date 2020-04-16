package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class RestoreRemoteBackupComplete {

    private AsyncResult _status;
    private Long _userAccountId;

    public static RestoreRemoteBackupComplete success(Long userAccountId) {
        RestoreRemoteBackupComplete result = new RestoreRemoteBackupComplete();
        result.setStatus(AsyncResult.Success);
        result.setUserAccountId(userAccountId);
        return result;
    }

    public static RestoreRemoteBackupComplete failure(AsyncResult status, Long userAccountId) {
        RestoreRemoteBackupComplete result = new RestoreRemoteBackupComplete();
        result.setStatus(status);
        result.setUserAccountId(userAccountId);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public Long getUserAccountId() {
        return _userAccountId;
    }

    public void setUserAccountId(Long userAccountId) {
        _userAccountId = userAccountId;
    }
}
