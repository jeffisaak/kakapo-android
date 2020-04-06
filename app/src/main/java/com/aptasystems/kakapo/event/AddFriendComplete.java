package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class AddFriendComplete {

    private AsyncResult _status;

    public static AddFriendComplete success() {
        AddFriendComplete result = new AddFriendComplete();
        result.setStatus(AsyncResult.Success);
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

}
