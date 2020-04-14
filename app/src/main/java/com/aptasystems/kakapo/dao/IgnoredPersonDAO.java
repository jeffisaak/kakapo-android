package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.IgnoredPerson;
import com.aptasystems.kakapo.entities.UserAccount;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

@Singleton
public class IgnoredPersonDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public IgnoredPersonDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public IgnoredPerson find(long userAccountId, String guid) {
        return _entityStore.select(IgnoredPerson.class)
                .where(IgnoredPerson.USER_ACCOUNT_ID.eq(userAccountId))
                .and(IgnoredPerson.GUID.eq(guid))
                .get()
                .firstOrNull();
    }

    // Multiple record lists.

    // Insertions.

    public void insert(UserAccount userAccount,
                                String guid) {
        IgnoredPerson ignoredPerson = new IgnoredPerson();
        ignoredPerson.setUserAccount(userAccount);
        ignoredPerson.setGuid(guid);
        _entityStore.insert(ignoredPerson);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccount.getId(), true);
    }

    // Deletions.

    public void delete(long userAccountId) {
        _entityStore.delete(IgnoredPerson.class)
                .where(IgnoredPerson.USER_ACCOUNT_ID.eq(userAccountId))
                .get()
                .value();

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    public void delete(long userAccountId, String guid) {
        _entityStore.delete(IgnoredPerson.class)
                .where(IgnoredPerson.USER_ACCOUNT_ID.eq(userAccountId))
                .and(IgnoredPerson.GUID.eq(guid))
                .get()
                .value();

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccountId, true);
    }

    // Updates.

    // Unsorted.

}
