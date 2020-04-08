package com.aptasystems.kakapo.dao;

import android.content.Context;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.service.AccountData;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.worker.AccountBackupWorker;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

@Singleton
public class UserAccountDAO {

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    Context _context;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    public UserAccountDAO(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    // Single record finds.

    public UserAccount find(Long id) {
        return _entityStore.select(UserAccount.class)
                .where(UserAccount.ID.eq(id))
                .get()
                .firstOrNull();
    }

    // Multiple record lists.

    public Result<UserAccount> list() {
        return _entityStore.select(UserAccount.class)
                .orderBy(UserAccount.NAME.asc(), UserAccount.ID.asc())
                .get();
    }

    // Insertions.

    public UserAccount insert(UserAccount userAccount) {
        _entityStore.insert(userAccount);
        return userAccount;
    }

    // Deletions.

    public void delete(UserAccount userAccount) {
        _entityStore.delete(userAccount);
    }

    // Updates.

    public void updateColour(long userAccountId, int colour) {
        _entityStore.update(UserAccount.class)
                .set(UserAccount.COLOUR, colour)
                .where(UserAccount.ID.eq(userAccountId))
                .get()
                .value();

        updateBackupRequired(userAccountId, true);
    }

    public void updateName(long userAccountId, String name) {
        _entityStore.update(UserAccount.class)
                .set(UserAccount.NAME, name)
                .where(UserAccount.ID.eq(userAccountId))
                .get()
                .value();

        updateBackupRequired(userAccountId, true);
    }

    public void updateBackupRequired(long userAccountId, boolean backupRequired) {
        _entityStore.update(UserAccount.class)
                .set(UserAccount.BACKUP_REQUIRED, backupRequired)
                .where(UserAccount.ID.eq(userAccountId))
                .get()
                .value();

        // FUTURE: Figure out a better home for this.
        // If we are setting the backup required flag, queue a unique work item delayed for ten
        // seconds. If we are clearing the backup required flag, cancel the unique work item.
        if (backupRequired) {
            Data accountBackupData = new Data.Builder()
                    .putLong(AccountBackupWorker.KEY_USER_ACCOUNT_ID, userAccountId)
                    .putString(AccountBackupWorker.KEY_PASSWORD, _prefsUtil.getCurrentPassword())
                    .build();
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AccountBackupWorker.class)
                    .setInputData(accountBackupData)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(_context).enqueueUniqueWork("backupAccountData-" + userAccountId,
                    ExistingWorkPolicy.REPLACE,
                    workRequest);
        } else {
            WorkManager.getInstance(_context).cancelUniqueWork("backupAccountData-" + userAccountId);
        }
    }

    public void updateRemoteBackupVersionNumber(long userAccountId, long versionNumber) {
        _entityStore.update(UserAccount.class)
                .set(UserAccount.REMOTE_BACKUP_VERSION_NUMBER, versionNumber)
                .where(UserAccount.ID.eq(userAccountId))
                .get()
                .value();

        updateBackupRequired(userAccountId, false);
    }

    public void clearRemoteBackupVersionNumber(long userAccountId) {
        _entityStore.update(UserAccount.class)
                .set(UserAccount.REMOTE_BACKUP_VERSION_NUMBER, null)
                .where(UserAccount.ID.eq(userAccountId))
                .get()
                .value();

        updateBackupRequired(userAccountId, true);
    }

    public void update(long userAccountId, AccountData accountData) {
        _entityStore.update(UserAccount.class)
                .set(UserAccount.NAME, accountData.getUserAccount().getName())
                .set(UserAccount.PASSWORD_SALT, accountData.getUserAccount().getPasswordSalt())
                .set(UserAccount.SIGNING_PUBLIC_KEY, accountData.getUserAccount().getSigningPublicKey())
                .set(UserAccount.ENCRYPTED_SIGNING_SECRET_KEY, accountData.getUserAccount().getEncryptedSigningSecretKey())
                .set(UserAccount.SIGNING_SECRET_KEY_NONCE, accountData.getUserAccount().getSigningSecretKeyNonce())
                .set(UserAccount.API_KEY, accountData.getUserAccount().getApiKey())
                .set(UserAccount.COLOUR, accountData.getUserAccount().getColour())
                .set(UserAccount.REMOTE_BACKUP_VERSION_NUMBER, accountData.getRemoteBackupVersionNumber())
                .where(UserAccount.ID.eq(userAccountId))
                .get()
                .value();

        updateBackupRequired(userAccountId, true);
    }

    // Unsorted.

}
