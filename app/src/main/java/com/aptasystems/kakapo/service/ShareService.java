package com.aptasystems.kakapo.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.ShareItemActivity;
import com.aptasystems.kakapo.adapter.model.NewsListItemState;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Share;
import com.aptasystems.kakapo.entities.ShareRecipient;
import com.aptasystems.kakapo.entities.ShareState;
import com.aptasystems.kakapo.entities.ShareType;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AttachmentDecryptComplete;
import com.aptasystems.kakapo.event.ContentStreamComplete;
import com.aptasystems.kakapo.event.ContentStreamProgress;
import com.aptasystems.kakapo.event.DeleteItemComplete;
import com.aptasystems.kakapo.event.FetchItemHeadersComplete;
import com.aptasystems.kakapo.event.NewsItemDecryptComplete;
import com.aptasystems.kakapo.event.ShareItemQueued;
import com.aptasystems.kakapo.event.SubmitItemComplete;
import com.aptasystems.kakapo.event.SubmitItemStarted;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.adapter.model.RegularNewsListItem;
import com.aptasystems.kakapo.adapter.model.ResponseNewsListItem;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.StringRes;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.model.ShareItem;
import kakapo.api.model.ShareItemRecipient;
import kakapo.api.request.DeleteItemRequest;
import kakapo.api.request.FetchItemHeadersRequest;
import kakapo.api.request.FetchRecipientsRequest;
import kakapo.api.request.StreamContentRequest;
import kakapo.api.request.SubmitItemRequest;
import kakapo.api.response.DeleteItemResponse;
import kakapo.api.response.FetchItemHeadersResponse;
import kakapo.api.response.FetchRecipientsResponse;
import kakapo.api.response.SubmitItemResponse;
import kakapo.client.model.BaseHeaderOrContent;
import kakapo.client.model.ItemDeserializeException;
import kakapo.client.model.ItemSerializeException;
import kakapo.client.ItemSerializer;
import kakapo.client.model.ResponseHeaderV1;
import kakapo.client.model.RegularContentV1;
import kakapo.client.model.RegularHeaderV1;
import kakapo.client.model.UnknownItemTypeException;
import kakapo.crypto.KeyPair;
import kakapo.crypto.PGPEncryptionService;
import kakapo.crypto.PGPRecipient;
import kakapo.crypto.exception.DecryptFailedException;
import kakapo.crypto.exception.EncryptFailedException;
import kakapo.crypto.exception.SignMessageException;
import kakapo.util.TimeUtil;
import okhttp3.ResponseBody;

@Singleton
public class ShareService {

    private static final String TAG = ShareService.class.getSimpleName();

    public static final int REQUEST_EDIT_RESPONSE_ITEM = 100;

    private Context _context;
    private PGPEncryptionService _pgpEncryptionService;
    private RetrofitWrapper _retrofitWrapper;
    private EntityDataStore<Persistable> _entityStore;
    private PrefsUtil _prefsUtil;
    private EventBus _eventBus;

    @Inject
    public ShareService(Context context,
                        PGPEncryptionService pgpEncryptionService,
                        RetrofitWrapper retrofitWrapper,
                        EntityDataStore<Persistable> entityStore,
                        PrefsUtil prefsUtil,
                        EventBus eventBus) {
        _context = context;
        _pgpEncryptionService = pgpEncryptionService;
        _retrofitWrapper = retrofitWrapper;
        _entityStore = entityStore;
        _prefsUtil = prefsUtil;
        _eventBus = eventBus;
    }

    public void showShareItemActivity(Activity activity, Long shareItemId) {
        Share shareItem = _entityStore.findByKey(Share.class, shareItemId);
        showShareItemActivity(activity, shareItem);
    }

    public void showShareItemActivity(Activity activity, Share shareItem) {

        switch (shareItem.getType()) {
            case RegularV1:
                showShareItemActivityRegularItem(activity, shareItem);
                break;
            case ResponseV1:
                showShareItemActivityResponseItem(activity, shareItem);
                break;
        }
    }

    private void showShareItemActivityRegularItem(Context context, Share shareItem) {

        // Convert the recipients into a map of GUIDs and names.
        HashMap<String, String> guidMap = new HashMap<>();
        for (ShareRecipient recipient : shareItem.getRecipients()) {

            Friend friend = _entityStore.select(Friend.class)
                    .where(Friend.GUID.eq(recipient.getGuid()))
                    .and(Friend.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                    .get().first();

            guidMap.put(friend.getGuid(), friend.getName());
        }
        Intent intent = new Intent(_context, ShareItemActivity.class);
        intent.putExtra(ShareItemActivity.EXTRA_KEY_ITEM_TYPE, shareItem.getType());
        intent.putExtra(ShareItemActivity.EXTRA_KEY_ITEM_ID, shareItem.getId());
        intent.putExtra(ShareItemActivity.EXTRA_KEY_RECIPIENTS, guidMap);
        intent.putExtra(ShareItemActivity.EXTRA_KEY_TITLE, shareItem.getTitle());
        if (shareItem.getUrl() != null) {
            intent.putExtra(ShareItemActivity.EXTRA_KEY_URL, shareItem.getUrl());
        }
        if (shareItem.getMessage() != null) {
            intent.putExtra(ShareItemActivity.EXTRA_KEY_MESSAGE, shareItem.getMessage());
        }
        if (shareItem.getAttachmentUri() != null) {
            intent.putExtra(ShareItemActivity.EXTRA_KEY_ATTACHMENT_URI, shareItem.getAttachmentUri());
        }
        if (shareItem.getMimeType() != null) {
            intent.putExtra(ShareItemActivity.EXTRA_KEY_MIME_TYPE, shareItem.getMimeType());
        }
        if (shareItem.getErrorMessage() != null) {
            intent.putExtra(ShareItemActivity.EXTRA_KEY_ERROR_MESSAGE, shareItem.getErrorMessage());
        }
        context.startActivity(intent);
    }

    private void showShareItemActivityResponseItem(Activity activity, Share shareItem) {
        Intent intent = new Intent(_context, ShareItemActivity.class);
        intent.putExtra(ShareItemActivity.EXTRA_KEY_ITEM_TYPE, shareItem.getType());
        intent.putExtra(ShareItemActivity.EXTRA_KEY_ITEM_ID, shareItem.getId());
        intent.putExtra(ShareItemActivity.EXTRA_KEY_RESPONSE, shareItem.getMessage());
        intent.putExtra(ShareItemActivity.EXTRA_KEY_PARENT_ITEM_REMOTE_ID, shareItem.getParentItemRemoteId());
        intent.putExtra(ShareItemActivity.EXTRA_KEY_ROOT_ITEM_REMOTE_ID, shareItem.getRootItemRemoteId());
        if (shareItem.getErrorMessage() != null) {
            intent.putExtra(ShareItemActivity.EXTRA_KEY_ERROR_MESSAGE, shareItem.getErrorMessage());
        }

        activity.startActivityForResult(intent, REQUEST_EDIT_RESPONSE_ITEM);
    }

    public long queueItem(long userAccountId, long parentItemRemoteId, long rootItemRemoteId, String response) {

        // Fetch the user account
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Build the share item.
        Share shareItem = new Share();
        shareItem.setType(ShareType.ResponseV1);
        shareItem.setMessage(response);
        shareItem.setParentItemRemoteId(parentItemRemoteId);
        shareItem.setRootItemRemoteId(rootItemRemoteId);
        shareItem.setUserAccount(userAccount);
        shareItem.setTimestampGmt(TimeUtil.timestampInGMT());
        shareItem.setState(ShareState.Queued);

        // Persist the share item.
        _entityStore.insert(shareItem);

        // Post an event
        _eventBus.post(new ShareItemQueued());

        return shareItem.getId();
    }

    public long queueItem(long userAccountId, Set<String> sharedWithGUIDs, String title, String url, String message, Uri attachmentUri, String mimeType) {

        // Fetch the user account
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Build the share item.
        Share shareItem = new Share();
        shareItem.setType(ShareType.RegularV1);
        shareItem.setTitle(title);
        shareItem.setUrl(url);
        shareItem.setMessage(message);
        shareItem.setUserAccount(userAccount);
        if (attachmentUri != null) {
            shareItem.setAttachmentUri(attachmentUri.toString());
        }
        if (mimeType != null) {
            shareItem.setMimeType(mimeType);
        }
        shareItem.setTimestampGmt(TimeUtil.timestampInGMT());
        shareItem.setState(ShareState.Queued);

        for (String guid : sharedWithGUIDs) {
            Friend friend = _entityStore.select(Friend.class)
                    .where(Friend.GUID.eq(guid))
                    .and(Friend.USER_ACCOUNT_ID.eq(userAccountId))
                    .get().first();

            ShareRecipient recipient = new ShareRecipient();
            recipient.setGuid(guid);
            recipient.setPublicKey(friend.getPublicKeyRingsData());

            shareItem.getRecipients().add(recipient);
        }

        // Persist the share item.
        _entityStore.insert(shareItem);

        // Post an event
        _eventBus.post(new ShareItemQueued());

        return shareItem.getId();
    }

    public void deleteQueuedItem(long itemId) {
        Share shareItem = _entityStore.findByKey(Share.class, itemId);
        _entityStore.delete(shareItem);
    }

    public Disposable submitItemAsync(Class<?> eventTarget,
                                      long shareItemId,
                                      String hashedPassword) {
        return Observable.fromCallable(() -> submitItem(shareItemId, hashedPassword))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(SubmitItemComplete.success(eventTarget,
                        result.getItemRemoteId(),
                        result.getUsedQuota(),
                        result.getMaxQuota())), throwable -> {

                    // When an error occurs during submission, we want to update the share's status
                    // in the database on the device and let the user know that something went
                    // sideways. We are not going to rely on the receiver of the failure event to
                    // update the record in the database, we'll do it here.

                    // Turn the status into an error message.
                    ApiException apiException = (ApiException) throwable;
                    @StringRes
                    int errorMessageId = 0;
                    switch (apiException.getErrorCode()) {
                        case IncorrectPassword:
                        case Unauthorized:
                            errorMessageId = R.string.fragment_me_item_error_unauthorized;
                            break;
                        case ItemSerializationFailed:
                            errorMessageId = R.string.fragment_me_item_error_item_serialization_failed;
                            break;
                        case EncryptionFailed:
                            errorMessageId = R.string.fragment_me_item_error_item_encryption_failed;
                            break;
                        case PayloadTooLarge:
                            errorMessageId = R.string.fragment_me_item_error_payload_too_large;
                            break;
                        case TooManyRequests:
                            errorMessageId = R.string.fragment_me_item_error_too_many_requests;
                            break;
                        case QuotaExceeded:
                            errorMessageId = R.string.fragment_me_snack_quota_exceeded;
                            break;
                        case OtherHttpError:
                            errorMessageId = R.string.fragment_me_item_error_other_http_error;
                            break;
                        case ServerUnavailable:
                            errorMessageId = R.string.fragment_me_item_error_server_unavailable;
                            break;
                        case RetrofitIOException:
                            errorMessageId = R.string.fragment_me_item_error_retrofit_io_exception;
                            break;
                    }

                    // Update the share item in the database with the status and error message.
                    Share shareItem = _entityStore.findByKey(Share.class, shareItemId);
                    shareItem.setState(ShareState.Error);
                    shareItem.setErrorMessage(_context.getResources().getString(errorMessageId));
                    _entityStore.update(shareItem);

                    // Post an event.
                    _eventBus.post(SubmitItemComplete.failure(eventTarget,
                            apiException.getErrorCode(),
                            shareItemId));
                });
    }

    private SubmitItemResponse submitItem(long shareItemId, String hashedPassword) throws ApiException {

        // Fetch the share item from the data store.
        Share shareItem = _entityStore.findByKey(Share.class, shareItemId);

        // Update the status of the share item and post an event.
        shareItem.setState(ShareState.Submitting);
        _entityStore.update(shareItem);
        _eventBus.post(new SubmitItemStarted(shareItemId));

        // We do different things depending on what kind of share item we have.
        switch (shareItem.getType()) {
            case RegularV1:
                return submitItem(shareItem, hashedPassword);
            case ResponseV1:

                if (shareItem.getRecipients().isEmpty()) {
                    // If the list of recipients is empty, we need to go off to the Kakapo server and
                    // get recipient GUIDs and public keys for the thing we're responding to (the parent
                    // item).
                    fetchRecipients(shareItem, hashedPassword);
                    return submitItem(shareItem, hashedPassword);
                } else {
                    // The list of recipients is not empty, so we can proceed normally.
                    return submitItem(shareItem, hashedPassword);
                }
            default:
                return null;
        }
    }

    private void fetchRecipients(Share shareItem, String hashedPassword) throws ApiException {

        // Build the request.
        FetchRecipientsRequest fetchRecipientsRequest = new FetchRecipientsRequest();
        fetchRecipientsRequest.setGuid(shareItem.getUserAccount().getGuid());
        fetchRecipientsRequest.setItemRemoteId(shareItem.getParentItemRemoteId());

        // Sign the request.
        try {
            KeyPair keyPair = new KeyPair(shareItem.getUserAccount().getSecretKeyRings(),
                    shareItem.getUserAccount().getPublicKeyRings());
            byte[] signature = _pgpEncryptionService.sign(fetchRecipientsRequest.getMessageDigest(),
                    keyPair,
                    shareItem.getUserAccount().getGuid(),
                    hashedPassword);
            fetchRecipientsRequest.setSignature(signature);
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Make HTTP call.
        FetchRecipientsResponse response = _retrofitWrapper.fetchRecipients(fetchRecipientsRequest);

        // Parse the recipients in the response and update the share item with recipient data.
        for (ShareItemRecipient recipient : response.getRecipients()) {
            ShareRecipient recipientEntity = new ShareRecipient();
            recipientEntity.setGuid(recipient.getGuid());
            recipientEntity.setPublicKey(recipient.getPublicKeyData());
            shareItem.getRecipients().add(recipientEntity);
        }
        _entityStore.update(shareItem);
    }

    private SubmitItemResponse submitItem(Share shareItem, String hashedPassword) throws ApiException {

        // Create a list of recipient GUIDs from the list of recipients.
        Set<String> recipientGUIDs = new HashSet<>();
        for (ShareRecipient recipient : shareItem.getRecipients()) {
            recipientGUIDs.add(recipient.getGuid());
        }

        // Add ourself to the list of recipient GUIDs. If we're already added, does nothing.
        recipientGUIDs.add(shareItem.getUserAccount().getGuid());

        // Serialise the share item.
        Pair<byte[], byte[]> serializedData;
        try {
            serializedData = shareItem.serialize(_context);
        } catch (IOException | UnknownItemTypeException | ItemSerializeException e) {
            throw new ApiException(e, AsyncResult.ItemSerializationFailed);
        }

        // Encrypt the share item.
        Pair<byte[], byte[]> encryptedData;
        try {
            encryptedData = encrypt(shareItem.getUserAccount(), shareItem.getRecipients(), serializedData, hashedPassword);
        } catch (EncryptFailedException e) {
            throw new ApiException(e, AsyncResult.EncryptionFailed);
        }

        // Build the request.
        SubmitItemRequest submitItemRequest = new SubmitItemRequest();
        submitItemRequest.setGuid(shareItem.getUserAccount().getGuid());
        submitItemRequest.setSharedWithGuids(recipientGUIDs);
        submitItemRequest.setParentItemRemoteId(shareItem.getParentItemRemoteId());

        // Sign the request.
        try {
            KeyPair keyPair = new KeyPair(shareItem.getUserAccount().getSecretKeyRings(),
                    shareItem.getUserAccount().getPublicKeyRings());
            byte[] signature = _pgpEncryptionService.sign(submitItemRequest.getMessageDigest(),
                    keyPair,
                    shareItem.getUserAccount().getGuid(),
                    hashedPassword);
            submitItemRequest.setSignature(signature);
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Make HTTP call.
        SubmitItemResponse response = _retrofitWrapper.submitItem(submitItemRequest, encryptedData.first, encryptedData.second);

        // If everything was successful until now, delete the item from the database.
        deleteQueuedItem(shareItem.getId());

        // Return the response.
        return response;
    }

    private Pair<byte[], byte[]> encrypt(UserAccount userAccount,
                                         Set<ShareRecipient> shareItemRecipients,
                                         Pair<byte[], byte[]> serializedData,
                                         String hashedPassword)
            throws EncryptFailedException {

        // Build recipients.
        List<PGPRecipient> recipients = new ArrayList<>();
        for (ShareRecipient shareRecipient : shareItemRecipients) {
            PGPRecipient recipient = new PGPRecipient(shareRecipient.getGuid(),
                    shareRecipient.getPublicKey());
            recipients.add(recipient);
        }

        // Add our own public key so that we can see our own items.
        recipients.add(new PGPRecipient(userAccount.getGuid(), userAccount.getPublicKeyRings()));

        // Perform the encryption.
        byte[] encryptedHeader = _pgpEncryptionService.encryptAndSign(serializedData.first,
                new KeyPair(userAccount.getSecretKeyRings(), userAccount.getPublicKeyRings()),
                userAccount.getGuid(),
                hashedPassword,
                recipients.toArray(new PGPRecipient[0]));
        byte[] encryptedContent = _pgpEncryptionService.encryptAndSign(serializedData.second,
                new KeyPair(userAccount.getSecretKeyRings(), userAccount.getPublicKeyRings()),
                userAccount.getGuid(),
                hashedPassword,
                recipients.toArray(new PGPRecipient[0]));

        // Return the result of the encryption.
        return new Pair<>(encryptedHeader, encryptedContent);
    }

    public Disposable deleteItemAsync(long itemRid, long userAccountId, String hashedPassword) {
        return Observable.fromCallable(() -> deleteItem(itemRid, userAccountId, hashedPassword))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(DeleteItemComplete.success(result.getItemRemoteId(), result.getUsedQuota(), result.getMaxQuota())), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(DeleteItemComplete.failure(apiException.getErrorCode()));
                });
    }

    private DeleteItemResponse deleteItem(long itemRid,
                                          long userAccountId,
                                          String hashedPassword)
            throws ApiException {

        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Build the request.
        DeleteItemRequest deleteItemRequest = new DeleteItemRequest();
        deleteItemRequest.setGuid(userAccount.getGuid());
        deleteItemRequest.setItemRemoteId(itemRid);

        // Sign the request.
        try {
            KeyPair keyPair = new KeyPair(userAccount.getSecretKeyRings(),
                    userAccount.getPublicKeyRings());
            byte[] signature = _pgpEncryptionService.sign(deleteItemRequest.getMessageDigest(),
                    keyPair,
                    userAccount.getGuid(),
                    hashedPassword);
            deleteItemRequest.setSignature(signature);
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Make HTTP call.
        return _retrofitWrapper.deleteItem(deleteItemRequest);
    }

    public Disposable fetchItemHeadersForParentAsync(Class<?> eventTarget,
                                                     long userAccountId,
                                                     String hashedPassword,
                                                     Long parentItemRid) {
        return fetchItemHeadersAsync(eventTarget,
                userAccountId,
                hashedPassword,
                null,
                parentItemRid,
                null);
    }

    public Disposable fetchItemHeaderAsync(Class<?> eventTarget,
                                           long userAccountId,
                                           String hashedPassword,
                                           Long itemRid) {
        return fetchItemHeadersAsync(eventTarget,
                userAccountId,
                hashedPassword,
                null,
                null,
                itemRid);
    }

    public Disposable fetchItemHeadersAsync(Class<?> eventTarget,
                                            long userAccountId,
                                            String hashedPassword,
                                            Long lastKnownItemRid) {
        return fetchItemHeadersAsync(eventTarget,
                userAccountId,
                hashedPassword,
                lastKnownItemRid,
                null,
                null);
    }

    private Disposable fetchItemHeadersAsync(Class<?> eventTarget, long userAccountId, String hashedPassword, Long lastKnownItemRid, Long parentItemRid, Long itemRid) {
        return Observable.fromCallable(() -> fetchItemHeaders(userAccountId, hashedPassword, lastKnownItemRid, parentItemRid, itemRid))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(FetchItemHeadersComplete.success(eventTarget, result.getShareItems(), result.getRemainingItemCount())), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(FetchItemHeadersComplete.failure(eventTarget, apiException.getErrorCode()));
                });
    }

    private FetchItemHeadersResponse fetchItemHeaders(long userAccountId,
                                                      String hashedPassword,
                                                      Long lastKnownItemRid,
                                                      Long parentItemRid,
                                                      Long itemRid) throws ApiException {

        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Create the fetch headers request.
        FetchItemHeadersRequest fetchItemHeadersRequest = new FetchItemHeadersRequest();
        fetchItemHeadersRequest.setGuid(userAccount.getGuid());
        if (parentItemRid == null && itemRid == null) {
            fetchItemHeadersRequest.setItemCount(_context.getResources().getInteger(R.integer.news_items_per_page));
        }
        fetchItemHeadersRequest.setLastKnownItemRemoteId(lastKnownItemRid);
        fetchItemHeadersRequest.setParentItemRemoteId(parentItemRid);
        fetchItemHeadersRequest.setItemRemoteId(itemRid);

        // Sign the request.
        try {
            KeyPair keyPair = new KeyPair(userAccount.getSecretKeyRings(),
                    userAccount.getPublicKeyRings());
            byte[] signature = _pgpEncryptionService.sign(fetchItemHeadersRequest.getMessageDigest(),
                    keyPair,
                    userAccount.getGuid(),
                    hashedPassword);
            fetchItemHeadersRequest.setSignature(signature);
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Make HTTP call.
        return _retrofitWrapper.fetchItemHeaders(fetchItemHeadersRequest);
    }

    public Disposable decryptShareItemHeaderAsync(Class<?> eventTarget,
                                                  long userAccountId,
                                                  String hashedPassword,
                                                  ShareItem shareItem) {
        return Observable.fromCallable(() ->
                decryptShareItemHeader(userAccountId, hashedPassword, shareItem))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(NewsItemDecryptComplete.success(eventTarget, result)), throwable -> {
                    throwable.printStackTrace();
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(NewsItemDecryptComplete.failure(eventTarget, shareItem.getRemoteId(), apiException.getErrorCode()));
                });
    }

    private AbstractNewsListItem decryptShareItemHeader(long userAccountId,
                                                        String hashedPassword,
                                                        ShareItem shareItem) throws ApiException {

        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Decrypt.

        byte[] headerData = null;
        if (shareItem.getEncryptedHeader() != null) {
            try {
                headerData = _pgpEncryptionService.decrypt(shareItem.getEncryptedHeader(),
                        new KeyPair(userAccount.getSecretKeyRings(), userAccount.getPublicKeyRings()),
                        hashedPassword);
            } catch (DecryptFailedException e) {
                throw new ApiException(AsyncResult.DecryptionFailed);
            }
        }

        // Deserialize the header, then build an object that can be fed to the news adapters.

        BaseHeaderOrContent header;
        try {
            header = new ItemSerializer().deserialize(headerData);
        } catch (ItemDeserializeException | UnknownItemTypeException e) {
            e.printStackTrace();
            throw new ApiException(AsyncResult.ItemDeserializationFailed);
        }

        AbstractNewsListItem newsItem = null;

        if (header instanceof RegularHeaderV1) {
            RegularHeaderV1 regularHeaderV1 = (RegularHeaderV1) header;
            newsItem = new RegularNewsListItem();
            RegularNewsListItem regularNewsListItem = (RegularNewsListItem) newsItem;
            regularNewsListItem.setTitle(regularHeaderV1.getTitle());
            regularNewsListItem.setUrl(regularHeaderV1.getUrl());
            regularNewsListItem.setMessage(regularHeaderV1.getMessage());
            regularNewsListItem.setThumbnailData(regularHeaderV1.getThumbnailData());
        } else if (header instanceof ResponseHeaderV1) {
            ResponseHeaderV1 responseHeaderV1 = (ResponseHeaderV1) header;
            newsItem = new ResponseNewsListItem();
            ResponseNewsListItem responseNewsListItem = (ResponseNewsListItem) newsItem;
            responseNewsListItem.setMessage(responseHeaderV1.getResponse());
        }

        // Populate the rest of the news item from the share item.
        newsItem.setRemoteId(shareItem.getRemoteId());
        newsItem.setOwnerGuid(shareItem.getOwnerGuid());
        newsItem.setParentItemRemoteId(shareItem.getParentItemRemoteId());
        newsItem.setItemTimestamp(shareItem.getItemTimestamp());

        // Set the state.
        if (shareItem.isMarkedAsDeleted()) {
            newsItem.setState(NewsListItemState.Deleted);
        } else {
            newsItem.setState(NewsListItemState.Decrypted);
        }

        return newsItem;
    }

    public Disposable streamItemContentAsync(long userAccountId, String hashedPassword, long itemRid) {
        return Observable.create((ObservableOnSubscribe<byte[]>) emitter -> {
            byte[] result = streamItemContent(emitter, userAccountId, hashedPassword, itemRid);
            emitter.onNext(result);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if( result != null ) {
                        _eventBus.post(ContentStreamComplete.success(result));
                    }
                }, throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(ContentStreamComplete.failure(apiException.getErrorCode()));
                });
//        return Observable.fromCallable(() -> streamItemContent(userAccountId, hashedPassword, itemRid))
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(result -> {
//                    _eventBus.post(ContentStreamComplete.success(result));
//                }, throwable -> {
//                    ApiException apiException = (ApiException) throwable;
//                    _eventBus.post(ContentStreamComplete.failure(apiException.getErrorCode()));
//                });
    }

    private byte[] streamItemContent(ObservableEmitter<byte[]> emitter,
                                     long userAccountId,
                                     String hashedPassword,
                                     long itemRid)
            throws ApiException {
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Create the stream content request.
        StreamContentRequest streamContentRequest = new StreamContentRequest();
        streamContentRequest.setGuid(userAccount.getGuid());
        streamContentRequest.setItemRemoteId(itemRid);

        // Sign the request.
        try {
            KeyPair keyPair = new KeyPair(userAccount.getSecretKeyRings(),
                    userAccount.getPublicKeyRings());
            byte[] signature = _pgpEncryptionService.sign(streamContentRequest.getMessageDigest(),
                    keyPair,
                    userAccount.getGuid(),
                    hashedPassword);
            streamContentRequest.setSignature(signature);
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Make HTTP call. An exception will be thrown if this fails, so we can proceed afterwards
        // as if things are successful.
        ResponseBody responseBody = _retrofitWrapper.streamItemContent(streamContentRequest);

        // Save the data while reporting progress.
        long contentLength = responseBody.contentLength();
        byte[] buffer = new byte[1024];
        InputStream inputStream = responseBody.byteStream();
        ByteArrayOutputStream encryptedContentStream = new ByteArrayOutputStream();
        try {
            int bytesRead;
            while ((bytesRead = IOUtils.read(inputStream, buffer)) > 0) {
                if (emitter.isDisposed()) {
                    return null;
                }
                encryptedContentStream.write(buffer, 0, bytesRead);
                encryptedContentStream.flush();
                _eventBus.post(new ContentStreamProgress(encryptedContentStream.size(), contentLength));
            }
            encryptedContentStream.close();
            return encryptedContentStream.toByteArray();
        } catch (IOException e) {
            throw new ApiException(AsyncResult.ContentStreamFailed);
        }
    }

    /**
     * Decrypt and deserialize the attachment from the encrypted content. Returns immediately and
     * posts an {@link AttachmentDecryptComplete} event.
     *
     * @param userAccountId
     * @param hashedPassword
     * @param encryptedContent
     */
    public Disposable decryptAttachmentAsync(long userAccountId,
                                             String hashedPassword,
                                             byte[] encryptedContent) {
        return Observable.fromCallable(() ->
                decryptAttachment(userAccountId,
                        hashedPassword,
                        encryptedContent))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(AttachmentDecryptComplete.success(result)), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AttachmentDecryptComplete.failure(apiException.getErrorCode()));
                });
    }

    private byte[] decryptAttachment(long userAccountId,
                                     String hashedPassword,
                                     byte[] encryptedContent)
            throws ApiException {

        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Decrypt.
        byte[] decryptedContent = null;
        if (encryptedContent != null) {
            try {
                decryptedContent = _pgpEncryptionService.decrypt(encryptedContent,
                        new KeyPair(userAccount.getSecretKeyRings(), userAccount.getPublicKeyRings()),
                        hashedPassword);
            } catch (DecryptFailedException e) {
                throw new ApiException(AsyncResult.DecryptionFailed);
            }
        }

        // Deserialize content
        BaseHeaderOrContent content;
        try {
            content = new ItemSerializer().deserialize(decryptedContent);
        } catch (ItemDeserializeException | UnknownItemTypeException e) {
            e.printStackTrace();
            throw new ApiException(AsyncResult.ItemDeserializationFailed);
        }

        if (content instanceof RegularContentV1) {
            return ((RegularContentV1) content).getAttachmentData();
        } else {
            return null;
        }
    }
}
