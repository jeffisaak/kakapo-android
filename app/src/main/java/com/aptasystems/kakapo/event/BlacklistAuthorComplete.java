package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class BlacklistAuthorComplete {

    private AsyncResult _status;
    private String _guid;

    public static BlacklistAuthorComplete success(String guid) {
        BlacklistAuthorComplete result = new BlacklistAuthorComplete();
        result.setStatus(AsyncResult.Success);
        result.setGuid(guid);
        return result;
    }

    public static BlacklistAuthorComplete failure(AsyncResult status) {
        BlacklistAuthorComplete result = new BlacklistAuthorComplete();
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
