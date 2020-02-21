package com.aptasystems.kakapo.adapter.model;

public class FriendGroupListItem {

    private long _groupId;
    private long _friendId;
    private String _groupName;
    private boolean _member;

    public FriendGroupListItem(long groupId, long memberId, String groupName, boolean member) {
        _groupId = groupId;
        _friendId = memberId;
        _groupName = groupName;
        _member = member;
    }

    public long getGroupId() {
        return _groupId;
    }

    public long getFriendId() {
        return _friendId;
    }

    public String getGroupName() {
        return _groupName;
    }

    public boolean isMember() {
        return _member;
    }

    public void setMember(boolean member) {
        _member = member;
    }
}

