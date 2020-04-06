package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.GroupDAO;
import com.aptasystems.kakapo.dao.GroupMemberDAO;
import com.aptasystems.kakapo.dao.IgnoredItemDAO;
import com.aptasystems.kakapo.dao.IgnoredPersonDAO;
import com.aptasystems.kakapo.dao.PreKeyDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.PreKey;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.FriendListUpdated;
import com.aptasystems.kakapo.event.GroupAdded;
import com.aptasystems.kakapo.event.GroupMembersChanged;
import com.aptasystems.kakapo.event.IgnoresChanged;
import com.aptasystems.kakapo.event.RestoreRemoteBackupComplete;
import com.aptasystems.kakapo.event.UserAccountColourChanged;
import com.aptasystems.kakapo.event.UserAccountRenamed;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.goterl.lazycode.lazysodium.LazySodium;
import com.goterl.lazycode.lazysodium.utils.Key;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.response.GetBackupVersionResponse;
import kakapo.crypto.ICryptoService;
import kakapo.crypto.exception.DecryptFailedException;
import okhttp3.ResponseBody;
import retrofit2.Response;

@Singleton
public class AccountRestoreService {

    private static final String TAG = AccountRestoreService.class.getSimpleName();

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    GroupMemberDAO _groupMemberDAO;

    @Inject
    GroupDAO _groupDAO;

    @Inject
    FriendDAO _friendDAO;

    @Inject
    IgnoredPersonDAO _ignoredPersonDAO;

    @Inject
    IgnoredItemDAO _ignoredItemDAO;

    @Inject
    PreKeyDAO _preKeyDAO;

    @Inject
    ICryptoService _cryptoService;

    @Inject
    RetrofitWrapper _retrofitWrapper;

    @Inject
    EventBus _eventBus;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    public AccountRestoreService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public Disposable checkAndMergeRemoteBackupAsync(Long userAccountId, String password) {

        return Maybe.fromCallable(() -> checkAndMergeRemoteBackup(userAccountId, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(accountData -> {
                    _eventBus.post(RestoreRemoteBackupComplete.success(userAccountId));
                }, throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(RestoreRemoteBackupComplete.failure(apiException.getErrorCode(),
                            userAccountId));
                });
    }

    private AccountData checkAndMergeRemoteBackup(long userAccountId, String password) throws ApiException {

        // Get the backup version from the server.
        GetBackupVersionResponse backupVersionResponse =
                _retrofitWrapper.getBackupVersion(userAccountId, password);
        long backupVersion = backupVersionResponse.getBackupVersion() != null ?
                backupVersionResponse.getBackupVersion() : 0;

        // If backup version is zero, there is no backup. Clear the user account backup version and
        // set the "backup required" flag.
        // This shouldn't happen, but let's handle it.
        if (backupVersion == 0) {
            _userAccountDAO.clearRemoteBackupVersionNumber(userAccountId);
        }

        AccountData accountData = null;

        // If the backup version from the server is greater than the local one, go get the backup
        // data from the server and perform a merge. Cripes!
        UserAccount userAccount = _userAccountDAO.find(userAccountId);
        if (backupVersion > 0 &&
                (userAccount.getRemoteBackupVersionNumber() == null ||
                        userAccount.getRemoteBackupVersionNumber() < backupVersion)) {

            // Fetch the remote backup
            Response<ResponseBody> streamAccountBackupResponse =
                    _retrofitWrapper.streamAccountBackup(userAccountId, password);

            // Save the data.
            ResponseBody responseBody = streamAccountBackupResponse.body();
            byte[] buffer = new byte[1024];
            InputStream inputStream = responseBody.byteStream();
            ByteArrayOutputStream accountDataStream = new ByteArrayOutputStream();
            try {
                int bytesRead;
                while ((bytesRead = IOUtils.read(inputStream, buffer)) > 0) {
                    accountDataStream.write(buffer, 0, bytesRead);
                    accountDataStream.flush();
                }
                accountDataStream.close();
            } catch (IOException e) {
                throw new ApiException(AsyncResult.ContentStreamFailed);
            }

            // Get the backup version, salt, and nonce from the headers.
            Long currentBackupVersion =
                    Long.valueOf(streamAccountBackupResponse.headers().get("Kakapo-Backup-Version-Number"));
            String nonce = streamAccountBackupResponse.headers().get("Kakapo-Backup-Nonce");

            try {
                accountData = decryptAccountData(accountDataStream.toByteArray(),
                        userAccount.getPasswordSalt(), nonce, password);
                accountData.setRemoteBackupVersionNumber(currentBackupVersion);
            } catch (IOException e) {
                throw new ApiException(e, AsyncResult.AccountDeserializationFailed);
            } catch (DecryptFailedException e) {
                throw new ApiException(e, AsyncResult.DecryptionFailed);
            }
        }

        if (accountData != null) {
            mergeBackup(accountData, userAccountId);
        }

        return accountData;
    }

    private void mergeBackup(AccountData accountData,
                             Long userAccountId) {

        UserAccount userAccount = _userAccountDAO.find(userAccountId);

        // Merge user account data.
        _userAccountDAO.update(userAccountId, accountData);
        _eventBus.post(new UserAccountRenamed(accountData.getUserAccount().getName()));
        _eventBus.post(new UserAccountColourChanged());

        // We're just going to do a blind replacement of local friends, groups, group members,
        // ignored items and ignored people with the remote data. Not the most user friendly thing,
        // but it's good enough for now.
        // FUTURE: Do a mmore intelligent merge of friends, groups, and shit.

        // That said, we will ADD any remote prekeys that we don't already have.

        // First clear out the group members, friends, groups, ignored people, and ignored items.
        for (Group group : userAccount.getGroups()) {
            _groupMemberDAO.deleteForGroup(group);
            _groupDAO.delete(group);
        }
        for (Friend friend : userAccount.getFriends()) {
            _friendDAO.delete(friend);
        }
        _ignoredPersonDAO.delete(userAccountId);
        _ignoredItemDAO.delete(userAccountId);

        // Friends.
        for (AccountData.Friend friend : accountData.getFriends()) {
            _friendDAO.insert(userAccount,
                    friend.getName(),
                    friend.getGuid(),
                    friend.getSigningPublicKey(),
                    friend.getColour());
        }
        // TODO: Not quite the right event, but it'll do for now.
        _eventBus.post(new FriendListUpdated());

        // Add groups and members.
        for (AccountData.Group group : accountData.getGroups()) {

            // Insert the group.
            Group insertedGroup = _groupDAO.insert(userAccount, group.getName());

            // Insert group member records as necessary.
            for (String memberGuid : group.getMemberGuids()) {
                Friend friend = _friendDAO.find(userAccountId, memberGuid);
                _groupMemberDAO.insert(friend, insertedGroup);
            }
        }

        // TODO: Not quite the right event, but it'll do for now.
        _eventBus.post(new GroupAdded());
        _eventBus.post(new GroupMembersChanged());

        // Ignored people.
        for (String ignoredGuid : accountData.getIgnoredUserGuids()) {
            _ignoredPersonDAO.insert(userAccount, ignoredGuid);
        }

        // Ignored items.
        for (Long ignoredItemRemoteId : accountData.getIgnoredItemRemoteIds()) {
            _ignoredItemDAO.insert(userAccount, ignoredItemRemoteId);
        }

        _eventBus.post(new IgnoresChanged());

        // Merge prekeys. Add any new ones.

        // Build a set of local prekey ids.
        Set<Long> localPreKeys = new HashSet<>();
        for (PreKey preKey : userAccount.getPreKeys()) {
            localPreKeys.add(preKey.getPreKeyId());
        }

        // Iterate over remote pre keys and insert any we don't have.
        for (AccountData.PreKey preKey : accountData.getPreKeys()) {
            if (!localPreKeys.contains(preKey.getPreKeyId())) {
                _preKeyDAO.insert(userAccount,
                        preKey.getPreKeyId(),
                        LazySodium.toBin(preKey.getPublicKey()),
                        Key.fromHexString(preKey.getSecretKey()));
            }
        }

        // Finally, set the backup required flag to false.
        _userAccountDAO.updateBackupRequired(userAccountId, false);
    }

    private AccountData decryptAccountData(byte[] encryptedAccountByteArray,
                                           String salt,
                                           String nonce,
                                           String password)
            throws IOException, DecryptFailedException {

        // Decrypt the encrypted data.
        byte[] serializedAccountData = _cryptoService.decryptAccountData(password,
                salt, nonce, encryptedAccountByteArray);

        // Deserialize the decrypted data.
        return deserializeAccount(serializedAccountData);
    }

    private AccountData deserializeAccount(byte[] serializedAccountData) throws IOException {

        // Open streams.
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedAccountData);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

        // Read the version number.
        int versionNumber = dataInputStream.readInt();

        // Based on the version number, instantiate our serializer and do the work.
        IAccountSerializer accountSerializer;
        if (versionNumber == 1) {
            accountSerializer = new AccountSerializerV1(_entityStore);
        } else {
            throw new IOException("Unable to locate account serializer for backup version " + versionNumber);
        }

        AccountData accountData = accountSerializer.deserializeUserAccountData(dataInputStream);
        dataInputStream.close();

        return accountData;
    }
}
