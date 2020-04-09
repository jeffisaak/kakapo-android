package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.CachedRegularItem;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.entities.UserAccount;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

@Singleton
public class CachedRegularItemDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    public CachedRegularItemDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    // Multiple record lists.

    public Result<CachedRegularItem> list() {
        return _entityStore.select(CachedRegularItem.class)
                .orderBy(CachedRegularItem.REMOTE_ID.asc())
                .get();
    }

    // Insertions.

    public CachedRegularItem insert(CachedRegularItem item) {
        _entityStore.insert(item);
        return item;
    }

    // Deletions.

    public void deleteAll() {
        _entityStore.delete(CachedRegularItem.class)
                .get()
                .value();
    }

    // Updates.

    // Unsorted.

}
