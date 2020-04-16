package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.Share;
import com.aptasystems.kakapo.entities.ShareState;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

@Singleton
public class ShareDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    public ShareDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public Share find(long id) {
        return _entityStore.select(Share.class)
                .where(Share.ID.eq(id))
                .get()
                .firstOrNull();
    }

    // Multiple record lists.

    public Result<Share> list(long userAccountId) {
        return _entityStore.select(Share.class)
                .where(Share.USER_ACCOUNT_ID.eq(userAccountId))
                .orderBy(Share.TIMESTAMP_GMT.asc())
                .get();
    }

    public Result<Share> list(long userAccountId, long rootItemRemoteId) {
        return _entityStore.select(Share.class)
                .where(Share.USER_ACCOUNT_ID.eq(userAccountId))
                .and(Share.ROOT_ITEM_REMOTE_ID.eq(rootItemRemoteId))
                .orderBy(Share.TIMESTAMP_GMT.asc())
                .get();
    }

    // Insertions.

    public Share insert(Share share) {
        return _entityStore.insert(share);
    }

    // Deletions.

    public void delete(long id) {
        _entityStore.delete(Share.class)
                .where(Share.ID.eq(id))
                .get()
                .value();
    }

    // Updates.

    public void updateError(Share share, String errorMessage) {
        _entityStore.update(Share.class)
                .set(Share.STATE, ShareState.Error)
                .set(Share.ERROR_MESSAGE, errorMessage)
                .get()
                .value();
    }

    public void updateSubmitting(Share share) {
        _entityStore.update(Share.class)
                .set(Share.STATE, ShareState.Submitting)
                .get()
                .value();
    }

    // Unsorted.

}
