package com.aptasystems.kakapo.event;

public class AddFriendRequested {

    private String _friendGuid;

    public AddFriendRequested(String friendGuid) {
        _friendGuid = friendGuid;
    }

    public String getFriendGuid() {
        return _friendGuid;
    }
}
