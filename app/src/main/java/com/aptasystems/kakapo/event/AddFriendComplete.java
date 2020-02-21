package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class AddFriendComplete {

    private AsyncResult _status;
    private String _guid;

    public static AddFriendComplete success(String guid) {
        AddFriendComplete result = new AddFriendComplete();
        result.setStatus(AsyncResult.Success);
        result.setGuid(guid);
        return result;
    }

    public static AddFriendComplete failure(AsyncResult status) {
        AddFriendComplete result = new AddFriendComplete();
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
