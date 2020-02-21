package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class AccountCreationComplete {

    private AsyncResult _status;
    private String _guid;

    public static AccountCreationComplete success(String guid) {
        AccountCreationComplete result = new AccountCreationComplete();
        result.setStatus(AsyncResult.Success);
        result.setGuid(guid);
        return result;
    }

    public static AccountCreationComplete failure(AsyncResult status) {
        AccountCreationComplete result = new AccountCreationComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public String getGuid() {
        return _guid;
    }

    public void setGuid(String guid) {
        _guid = guid;
    }
}
