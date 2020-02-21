package com.aptasystems.kakapo.event;

public class FriendRenamed {

    private String _guid;
private String _name;

    public FriendRenamed(String guid, String name) {
        _guid = guid;
        _name = name;
    }

    public String getGuid() {
        return _guid;
    }

    public String getName() {
        return _name;
    }
}

