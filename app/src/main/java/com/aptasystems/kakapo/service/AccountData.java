package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.IgnoredItem;
import com.aptasystems.kakapo.entities.IgnoredPerson;
import com.aptasystems.kakapo.entities.UserAccount;

import java.util.List;

/**
 * POJO to contain deserialized account data.
 */
public class AccountData {

    private long _timestampInGmt;
    private UserAccount _userAccount;
    private List<Friend> _friends;
    private List<Group> _groups;
    private List<AccountRestoreService.GroupMemberMapping> _groupMembers;
    private List<IgnoredPerson> _ignoredPeople;
    private List<IgnoredItem> _ignoredItems;

    public long getTimestampInGmt() {
        return _timestampInGmt;
    }

    public void setTimestampInGmt(long timestampInGmt) {
        _timestampInGmt = timestampInGmt;
    }

    public UserAccount getUserAccount() {
        return _userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        _userAccount = userAccount;
    }

    public List<Friend> getFriends() {
        return _friends;
    }

    public void setFriends(List<Friend> friends) {
        _friends = friends;
    }

    public List<Group> getGroups() {
        return _groups;
    }

    public void setGroups(List<Group> groups) {
        _groups = groups;
    }

    public List<AccountRestoreService.GroupMemberMapping> getGroupMembers() {
        return _groupMembers;
    }

    public void setGroupMembers(List<AccountRestoreService.GroupMemberMapping> groupMembers) {
        _groupMembers = groupMembers;
    }

    public List<IgnoredPerson> getIgnoredPeople() {
        return _ignoredPeople;
    }

    public void setIgnoredPeople(List<IgnoredPerson> ignoredPeople) {
        _ignoredPeople = ignoredPeople;
    }

    public List<IgnoredItem> getIgnoredItems() {
        return _ignoredItems;
    }

    public void setIgnoredItems(List<IgnoredItem> ignoredItems) {
        _ignoredItems = ignoredItems;
    }
}
