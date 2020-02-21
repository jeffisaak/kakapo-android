package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.entities.IgnoredItem;
import com.aptasystems.kakapo.entities.IgnoredPerson;
import com.aptasystems.kakapo.entities.Share;
import com.aptasystems.kakapo.event.DownloadAccountComplete;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.collection.LongSparseArray;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.request.DownloadAccountRequest;
import kakapo.api.response.DownloadAccountResponse;
import kakapo.crypto.SecretKeyEncryptionService;
import kakapo.crypto.exception.CryptoException;
import kakapo.crypto.exception.DecryptFailedException;
import kakapo.crypto.exception.EncryptFailedException;
import kakapo.crypto.exception.KeyGenerationException;
import kakapo.util.HashUtil;

@Singleton
public class AccountRestoreService {

    private static final String TAG = AccountRestoreService.class.getSimpleName();

    private EntityDataStore<Persistable> _entityStore;
    private SecretKeyEncryptionService _secretKeyEncryptionService;
    private RetrofitWrapper _retrofitWrapper;
    private EventBus _eventBus;

    @Inject
    public AccountRestoreService(EntityDataStore<Persistable> entityStore,
                                 SecretKeyEncryptionService secretKeyEncryptionService,
                                 RetrofitWrapper retrofitWrapper,
                                 EventBus eventBus) {
        _entityStore = entityStore;
        _secretKeyEncryptionService = secretKeyEncryptionService;
        _retrofitWrapper = retrofitWrapper;
        _eventBus = eventBus;
    }

    /**
     * Asynchronously download and restore an account share from the Kakapo servers for the given
     * share guid, password, and salt. This method posts a {@link DownloadAccountComplete} event.
     *
     * @param accountBackupInfo
     * @return
     */
    public Disposable downloadAccountShareAsync(AccountBackupInfo accountBackupInfo) {
        return Completable.fromCallable(() -> downloadAccountShare(accountBackupInfo))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> _eventBus.post(DownloadAccountComplete.success()), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(DownloadAccountComplete.failure(apiException.getErrorCode()));
                });
    }

    /**
     * Download an account share from the Kakapo server for the given backup guid, password, and
     * salt.
     *
     * @param accountBackupInfo
     * @return
     * @throws ApiException
     */
    private Void downloadAccountShare(AccountBackupInfo accountBackupInfo) throws ApiException {

        // Build the request and make the HTTP call.
        DownloadAccountRequest downloadAccountRequest = new DownloadAccountRequest();
        downloadAccountRequest.setGuid(accountBackupInfo.getBackupGuid());
        DownloadAccountResponse response = _retrofitWrapper.downloadAccount(downloadAccountRequest);

        // Decrypt the encrypted account data.
        AccountData accountData;
        try {
            accountData = decryptAccountData(response.getEncryptedAccountData(),
                    accountBackupInfo.getPassword(),
                    accountBackupInfo.getSalt());
        } catch (CryptoException e) {
            throw new ApiException(AsyncResult.DecryptionFailed);
        } catch (IOException e) {
            throw new ApiException(AsyncResult.AccountDeserializationFailed);
        }

        // Restore the account.
        restore(accountData);

        return null;
    }

    /**
     * Restore account data to the device. Share items that have not been uploaded and are being
     * stored on the device either in error or queued state will not be restored as they are not
     * part of a backup.
     *
     * @param accountData
     */
    public void restore(AccountData accountData) {

        // First the user account.
        _entityStore.insert(accountData.getUserAccount());

        // Then the groups. Build a mapping of old group ids to new group ids as we go.
        LongSparseArray<Long> groupIdMap = new LongSparseArray<>();
        for (Group group : accountData.getGroups()) {
            long oldGroupId = group.getId();
            group.setUserAccount(accountData.getUserAccount());
            Group insertedGroup = _entityStore.insert(group);
            groupIdMap.put(oldGroupId, insertedGroup.getId());
        }

        // Friends. Build a mapping of old friend ids to new friend ids as we go.
        LongSparseArray<Long> friendIdMap = new LongSparseArray<>();
        for (Friend friend : accountData.getFriends()) {
            long oldFriendId = friend.getId();
            friend.setUserAccount(accountData.getUserAccount());
            Friend insertedFriend = _entityStore.insert(friend);
            friendIdMap.put(oldFriendId, insertedFriend.getId());
        }

        // Group members. Use the friend and group id maps to get the new ids.
        for (GroupMemberMapping groupMember : accountData.getGroupMembers()) {
            long newFriendId = friendIdMap.get(groupMember.getFriendId());
            long newGroupId = groupIdMap.get(groupMember.getGroupId());

            Friend friend = _entityStore.findByKey(Friend.class, newFriendId);
            Group group = _entityStore.findByKey(Group.class, newGroupId);

            GroupMember newGroupMember = new GroupMember();
            newGroupMember.setGroup(group);
            newGroupMember.setFriend(friend);
            _entityStore.insert(newGroupMember);
        }

        // Ignored people.
        for (IgnoredPerson ignoredPerson : accountData.getIgnoredPeople()) {
            ignoredPerson.setUserAccount(accountData.getUserAccount());
            _entityStore.insert(ignoredPerson);
        }

        // Ignored items.
        for (IgnoredItem ignoredItem : accountData.getIgnoredItems()) {
            ignoredItem.setUserAccount(accountData.getUserAccount());
            _entityStore.insert(ignoredItem);
        }
    }

    /**
     * Decrypt and deserialize encrypted account data to an {@link AccountData} object. The
     * resultant AccountData object does not contain queued {@link Share} entities.
     *
     * @param encryptedAccountByteArray
     * @param password
     * @return
     * @throws EncryptFailedException
     * @throws IOException
     */
    public AccountData decryptAccountData(byte[] encryptedAccountByteArray,
                                          String password)
            throws IOException, KeyGenerationException, DecryptFailedException {

        // Somewhat kludgy - we need a salt, and on the encryption side we've just sha256ed the
        // password, so we'll do that again here.
        String salt = HashUtil.sha256ToString(password);
        return decryptAccountData(encryptedAccountByteArray, password, salt);
    }

    private AccountData decryptAccountData(byte[] encryptedAccountByteArray,
                                           String password,
                                           String salt)
            throws IOException, KeyGenerationException, DecryptFailedException {

        // Decrypt the encrypted data.
        byte[] serializedAccountData =
                _secretKeyEncryptionService.decryptToByteArray(password,
                        salt,
                        encryptedAccountByteArray);

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

    static class GroupMemberMapping {
        private long _groupId;
        private long _friendId;

        long getGroupId() {
            return _groupId;
        }

        void setGroupId(long groupId) {
            _groupId = groupId;
        }

        long getFriendId() {
            return _friendId;
        }

        void setFriendId(long friendId) {
            _friendId = friendId;
        }
    }
}
