package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

@Singleton
public class GroupMemberDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public GroupMemberDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public GroupMember find(Long friendId, Long groupId) {
        return _entityStore.select(GroupMember.class)
                .where(GroupMember.FRIEND_ID.eq(friendId))
                .and(GroupMember.GROUP_ID.eq(groupId))
                .get().firstOrNull();
    }

    // Multiple record lists.

    public Result<GroupMember> listForGroup(long groupId) {
        return _entityStore.select(GroupMember.class)
                .where(GroupMember.GROUP_ID.eq(groupId))
                .get();
    }

    // Insertions.

    public void insert(Friend friend, Group group) {

        GroupMember groupMember = new GroupMember();
        groupMember.setFriend(friend);
        groupMember.setGroup(group);
        _entityStore.insert(groupMember);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(friend.getUserAccount().getId(), true);
    }

    // Deletions.

    public void delete(Friend friend, Group group) {

        // Save the user account id.
        long userAccountId = friend.getUserAccount().getId();

        // Delete the group member.
        _entityStore.delete(GroupMember.class)
                .where(GroupMember.FRIEND_ID.eq(friend.getId()))
                .and(GroupMember.GROUP_ID.eq(group.getId()))
                .get()
                .value();

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    public void deleteForGroup(Group group) {

        // Save the user account id.
        long userAccountId = group.getUserAccount().getId();

        // Delete the group member.
        _entityStore.delete(GroupMember.class)
                .where(GroupMember.GROUP_ID.eq(group.getId()))
                .get()
                .value();

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    public void deleteForFriend(Friend friend) {

        // Save the user account id.
        long userAccountId = friend.getUserAccount().getId();

        _entityStore.delete(GroupMember.class)
                .where(GroupMember.FRIEND_ID.eq(friend.getId()))
                .get()
                .value();

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    // Updates.

    // Unsorted.

    public int count(Long groupId) {
        return _entityStore.count(GroupMember.class)
                .where(GroupMember.GROUP_ID.eq(groupId))
                .get()
                .value();
    }
}
