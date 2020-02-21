package com.aptasystems.kakapo.adapter.model;

public class GroupMemberListItem {

    private long _groupId;
    private long _friendId;
    private String _friendName;
    private String _friendGuid;
    private boolean _member;

    public GroupMemberListItem(long groupId, long friendId, String friendName, String friendGuid, boolean member) {
        _groupId = groupId;
        _friendId = friendId;
        _friendName = friendName;
        _friendGuid = friendGuid;
        _member = member;
    }

    public long getGroupId() {
        return _groupId;
    }

    public long getFriendId() {
        return _friendId;
    }

    public String getFriendName() {
        return _friendName;
    }

    public String getFriendGuid() {
        return _friendGuid;
    }

    public boolean isMember() {
        return _member;
    }

    public void setMember(boolean member) {
        _member = member;
    }
}