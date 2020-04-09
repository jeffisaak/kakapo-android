package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.UserAccount;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

@Singleton
public class GroupDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public GroupDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public Group find(Long id) {
        return _entityStore.select(Group.class)
                .where(Group.ID.eq(id))
                .get()
                .firstOrNull();
    }

    // Multiple record lists.

    public Result<Group> list(Long userAccountId) {
        return _entityStore.select(Group.class)
                .where(Group.USER_ACCOUNT_ID.eq(userAccountId))
                .orderBy(Group.NAME.asc(), Group.ID.asc())
                .get();
    }

    // Insertions.

    public Group insert(UserAccount userAccount,
                        String name) {
        Group group = new Group();
        group.setUserAccount(userAccount);
        group.setName(name);
        Group insertedGroup = _entityStore.insert(group);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccount.getId(), true);

        return insertedGroup;
    }

    // Deletions.

    public void delete(Group group) {

        // Save the user account id.
        long userAccountId = group.getUserAccount().getId();

        // Delete the group.
        _entityStore.delete(group);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    // Updates.

    public void updateName(long groupId, String name) {

        // Set the name.
        _entityStore.update(Group.class)
                .set(Group.NAME, name)
                .where(Group.ID.eq(groupId))
                .get()
                .value();

        // Update the backup required flag.
        Group group = find(groupId);
        _userAccountDAO.updateBackupRequired(group.getUserAccount().getId(), true);
    }

    // Unsorted.

}
