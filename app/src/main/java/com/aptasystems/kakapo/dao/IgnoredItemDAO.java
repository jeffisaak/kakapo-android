package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.IgnoredItem;
import com.aptasystems.kakapo.entities.IgnoredPerson;
import com.aptasystems.kakapo.entities.UserAccount;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

@Singleton
public class IgnoredItemDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public IgnoredItemDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public IgnoredItem find(long userAccountId, Long itemRemoteId) {
        return _entityStore.select(IgnoredItem.class)
                .where(IgnoredItem.USER_ACCOUNT_ID.eq(userAccountId))
                .and(IgnoredItem.ITEM_REMOTE_ID.eq(itemRemoteId))
                .get()
                .firstOrNull();
    }

    // Multiple record lists.

    // Insertions.

    public void insert(UserAccount userAccount,
                       Long itemRemoteId) {

        IgnoredItem ignoredItem = new IgnoredItem();
        ignoredItem.setUserAccount(userAccount);
        ignoredItem.setItemRemoteId(itemRemoteId);
        _entityStore.insert(ignoredItem);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccount.getId(), true);
    }

    // Deletions.

    public void delete(long userAccountId) {

        _entityStore.delete(IgnoredItem.class)
                .where(IgnoredItem.USER_ACCOUNT_ID.eq(userAccountId))
                .get()
                .value();

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    public void delete(long userAccountId, long itemRemoteId) {

        _entityStore.delete(IgnoredItem.class)
                .where(IgnoredItem.USER_ACCOUNT_ID.eq(userAccountId))
                .and(IgnoredItem.ITEM_REMOTE_ID.eq(itemRemoteId))
                .get()
                .value();

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    // Updates.

    // Unsorted.

}
