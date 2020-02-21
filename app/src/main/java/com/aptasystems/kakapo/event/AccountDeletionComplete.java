package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class AccountDeletionComplete {

    private boolean _deleteFromServer;
    private AsyncResult _status;

    public static AccountDeletionComplete success(boolean deleteFromServer) {
        AccountDeletionComplete result = new AccountDeletionComplete();
        result.setDeleteFromServer(deleteFromServer);
        result.setStatus(AsyncResult.Success);
        return result;
    }

    public static AccountDeletionComplete failure(AsyncResult status) {
        AccountDeletionComplete result = new AccountDeletionComplete();
        result.setStatus(status);
        return result;
    }

    public boolean isDeleteFromServer() {
        return _deleteFromServer;
    }

    public void setDeleteFromServer(boolean deleteFromServer) {
        _deleteFromServer = deleteFromServer;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }
}
