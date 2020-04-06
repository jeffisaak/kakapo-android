package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.Share;
import com.aptasystems.kakapo.entities.ShareRecipient;
import com.aptasystems.kakapo.entities.ShareState;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

@Singleton
public class ShareRecipientDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    public ShareRecipientDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    // Multiple record lists.

    // Insertions.

    public ShareRecipient insert(ShareRecipient shareRecipient) {
        _entityStore.insert(shareRecipient);
        return shareRecipient;
    }

    // Deletions.

    // Updates.

    public void updatePreKey(long shareRecipientId, Long preKeyId, String preKey) {
        _entityStore.update(ShareRecipient.class)
                .set(ShareRecipient.PRE_KEY, preKey)
                .set(ShareRecipient.PRE_KEY_ID, preKeyId)
                .where(ShareRecipient.ID.eq(shareRecipientId))
                .get()
                .value();
    }

    // Unsorted.

}
