package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AddFriendComplete;
import com.aptasystems.kakapo.event.FriendDeleted;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.util.ColourUtil;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.request.FetchPublicKeyRequest;
import kakapo.api.response.FetchPublicKeyResponse;
import kakapo.crypto.KeyPair;
import kakapo.crypto.PGPEncryptionService;
import kakapo.crypto.exception.SignMessageException;

@Singleton
public class FriendService {

    private PGPEncryptionService _pgpEncryptionService;
    private ColourUtil _colourUtil;
    private EntityDataStore<Persistable> _entityStore;
    private EventBus _eventBus;
    private RetrofitWrapper _retrofitWrapper;

    @Inject
    public FriendService( PGPEncryptionService pgpEncryptionService,
                          ColourUtil colourUtil,
                          EntityDataStore<Persistable> entityStore,
                          EventBus eventBus,
                          RetrofitWrapper retrofitWrapper) {
        _pgpEncryptionService = pgpEncryptionService;
        _colourUtil = colourUtil;
        _entityStore = entityStore;
        _eventBus = eventBus;
        _retrofitWrapper = retrofitWrapper;
    }

    public void deleteFriend(Friend friend) {
        String friendGuid = friend.getGuid();

        // Delete any group member entities that reference this friend.
        _entityStore.delete(GroupMember.class)
                .where(GroupMember.FRIEND_ID.eq(friend.getId()))
                .get()
                .value();

        // Delete the friend.
        _entityStore.delete(friend);

        // Post an event.
        _eventBus.post(new FriendDeleted(friendGuid));
    }

    public Disposable addFriendAsync(long userAccountId, String hashedPassword, String name, String guid) {

        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        return Observable.fromCallable(() -> fetchPublicKey(userAccount, hashedPassword, guid))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {

                    // Put the record in the database.
                    Friend friend = new Friend();
                    friend.setPublicKeyRingsData(result.getPublicKeyRings());
                    friend.setName(name);
                    friend.setGuid(guid);
                    friend.setColour(_colourUtil.randomColour());

                    userAccount.getFriends().add(friend);
                    _entityStore.update(userAccount);

                    _eventBus.post(AddFriendComplete.success(guid));
                }, throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AddFriendComplete.failure(apiException.getErrorCode()));
                });
    }

    private FetchPublicKeyResponse fetchPublicKey(UserAccount userAccount, String hashedPassword, String targetGuid) throws ApiException {

        // Build the request.
        FetchPublicKeyRequest request = new FetchPublicKeyRequest();
        request.setGuid(userAccount.getGuid());
        request.setTargetGuid(targetGuid);

        // Sign the request.
        try {
            KeyPair keyPair = new KeyPair(userAccount.getSecretKeyRings(), userAccount.getPublicKeyRings());
            byte[] signature = _pgpEncryptionService.sign(request.getMessageDigest(),
                    keyPair,
                    userAccount.getGuid(),
                    hashedPassword);
            request.setSignature(signature);
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Make HTTP call.
        return _retrofitWrapper.fetchPublicKey(request);
    }
}
