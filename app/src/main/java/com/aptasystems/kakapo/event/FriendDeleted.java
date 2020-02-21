package com.aptasystems.kakapo.event;

public class FriendDeleted {

    private String _guid;

    public FriendDeleted(String guid) {
        _guid = guid;
    }

    public String getGuid() {
        return _guid;
    }
}