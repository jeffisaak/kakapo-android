package com.aptasystems.kakapo.dao;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.PreKey;
import com.aptasystems.kakapo.entities.UserAccount;
import com.goterl.lazycode.lazysodium.LazySodium;
import com.goterl.lazycode.lazysodium.utils.Key;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

@Singleton
public class PreKeyDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public PreKeyDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public PreKey find(Long userAccountId, Long preKeyId) {
        return _entityStore.select(PreKey.class)
                .where(PreKey.USER_ACCOUNT_ID.eq(userAccountId))
                .and(PreKey.PRE_KEY_ID.eq(preKeyId))
                .get().firstOrNull();
    }

    // Multiple record lists.

    // Insertions.

    public void insert(UserAccount userAccount, Long preKeyId, byte[] publicKey, Key secretKey) {

        PreKey preKey = new PreKey();
        preKey.setUserAccount(userAccount);
        preKey.setPreKeyId(preKeyId);
        String preKeyPublicKey = LazySodium.toHex(publicKey);
        preKey.setPublicKey(preKeyPublicKey);
        preKey.setSecretKey(secretKey.getAsHexString());
        preKey.setUserAccount(userAccount);
        _entityStore.insert(preKey);

        // Update the backup required flag.
        _userAccountDAO.updateBackupRequired(userAccount.getId(), true);
    }

    // Deletions.

    // Updates.

    // Unsorted.

}
