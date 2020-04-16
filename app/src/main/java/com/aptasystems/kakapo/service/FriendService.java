package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.GroupMemberDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AddFriendComplete;
import com.aptasystems.kakapo.event.FriendDeleted;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.util.ColourUtil;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class FriendService {

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    GroupMemberDAO _groupMemberDAO;

    @Inject
    FriendDAO _friendDAO;

    @Inject
    ColourUtil _colourUtil;

    @Inject
    EventBus _eventBus;

    @Inject
    RetrofitWrapper _retrofitWrapper;

    @Inject
    public FriendService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public void deleteFriend(Friend friend) {
        String friendGuid = friend.getGuid();

        // Delete any group member entities that reference this friend.
        _groupMemberDAO.deleteForFriend(friend);

        // Delete the friend.
        _friendDAO.delete(friend);

        // Post an event.
        _eventBus.post(new FriendDeleted(friendGuid));
    }

    public Disposable addFriendAsync(long userAccountId, String password, String name, String targetGuid) {

        return Observable.fromCallable(() -> _retrofitWrapper.fetchPublicKey(targetGuid, userAccountId, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {

                    // Put the record in the database.
                    UserAccount userAccount = _userAccountDAO.find(userAccountId);
                    _friendDAO.insert(userAccount,
                            name,
                            targetGuid,
                            result.getSigningPublicKey(),
                            _colourUtil.randomColour());

                    // Post an event.
                    _eventBus.post(AddFriendComplete.success());

                }, throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AddFriendComplete.failure(apiException.getErrorCode()));
                });
    }
}
