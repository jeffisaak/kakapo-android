package com.aptasystems.kakapo.event;

public class FriendColourChanged {

    private long _friendId;
    private int _colour;

    public FriendColourChanged(long friendId, int colour) {
        _friendId = friendId;
        _colour = colour;
    }

    public long getFriendId() {
        return _friendId;
    }

    public int getColour() {
        return _colour;
    }
}
