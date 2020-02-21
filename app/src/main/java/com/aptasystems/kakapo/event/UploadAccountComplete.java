package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.service.AccountBackupInfo;
import com.aptasystems.kakapo.exception.AsyncResult;

public class UploadAccountComplete {

    private AsyncResult _status;
    private AccountBackupInfo _accountBackupInfo;

    public static UploadAccountComplete success(AccountBackupInfo accountBackupInfo) {
        UploadAccountComplete result = new UploadAccountComplete();
        result.setStatus(AsyncResult.Success);
        result.setAccountBackupInfo(accountBackupInfo);
        return result;
    }

    public static UploadAccountComplete failure(AsyncResult status) {
        UploadAccountComplete result = new UploadAccountComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public AccountBackupInfo getAccountBackupInfo() {
        return _accountBackupInfo;
    }

    public void setAccountBackupInfo(AccountBackupInfo accountBackupInfo) {
        _accountBackupInfo = accountBackupInfo;
    }
}
