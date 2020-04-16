package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.UserAccount;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

@Singleton
public class FriendDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public FriendDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public Friend find(Long id) {
        return _entityStore.select(Friend.class)
                .where(Friend.ID.eq(id))
                .get()
                .firstOrNull();
    }

    public Friend find(Long userAccountId, String friendGuid) {
        return _entityStore.select(Friend.class)
                .where(Friend.GUID.eq(friendGuid))
                .and(Friend.USER_ACCOUNT_ID.eq(userAccountId))
                .get().firstOrNull();
    }

    // Multiple record lists.

    public Result<Friend> list(Long userAccountId) {
        return _entityStore.select(Friend.class)
                .where(Friend.USER_ACCOUNT_ID.eq(userAccountId))
                .orderBy(Friend.NAME.asc(), Friend.ID.asc())
                .get();
    }

    // Insertions.

    public Friend insert(UserAccount userAccount,
                         String name,
                         String guid,
                         String signingPublicKey,
                         int colour) {

        // Build and insert the friend record.
        Friend friend = new Friend();
        friend.setUserAccount(userAccount);
        friend.setName(name);
        friend.setGuid(guid);
        friend.setSigningPublicKey(signingPublicKey);
        friend.setColour(colour);
        Friend insertedFriend = _entityStore.insert(friend);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccount.getId(), true);

        return insertedFriend;
    }

    // Deletions.

    public void delete(Friend friend) {

        // Save the user account id.
        long userAccountId = friend.getUserAccount().getId();

        // Delete the friend.
        _entityStore.delete(friend);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    // Updates.

    public void updateColour(long friendId, int colour) {

        // Set the colour.
        _entityStore.update(Friend.class)
                .set(Friend.COLOUR, colour)
                .where(Friend.ID.eq(friendId))
                .get()
                .value();

        // Update the backup required flag.
        Friend friend = find(friendId);
        _userAccountDAO.updateBackupRequired(friend.getUserAccount().getId(), true);
    }

    public void updateName(long friendId, String name) {

        // Set the name.
        _entityStore.update(Friend.class)
                .set(Friend.NAME, name)
                .where(Friend.ID.eq(friendId))
                .get()
                .value();

        // Update the backup required flag.
        Friend friend = find(friendId);
        _userAccountDAO.updateBackupRequired(friend.getUserAccount().getId(), true);
    }

    // Unsorted.

}
