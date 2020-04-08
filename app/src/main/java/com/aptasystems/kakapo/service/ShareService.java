package com.aptasystems.kakapo.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.ShareItemActivity;
import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.adapter.model.NewsListItemState;
import com.aptasystems.kakapo.adapter.model.RegularNewsListItem;
import com.aptasystems.kakapo.adapter.model.ResponseNewsListItem;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.PreKeyDAO;
import com.aptasystems.kakapo.dao.ShareDAO;
import com.aptasystems.kakapo.dao.ShareRecipientDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.PreKey;
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
import com.aptasystems.kakapo.util.PrefsUtil;
import com.goterl.lazycode.lazysodium.LazySodium;
import com.goterl.lazycode.lazysodium.utils.Key;
import com.goterl.lazycode.lazysodium.utils.KeyPair;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import kakapo.api.model.SubmitItemDestination;
import kakapo.api.request.SubmitItemRequest;
import kakapo.api.response.DeleteItemResponse;
import kakapo.api.response.FetchItemHeadersResponse;
import kakapo.api.response.FetchPreKeyResponse;
import kakapo.api.response.FetchRecipientsResponse;
import kakapo.api.response.SubmitItemResponse;
import kakapo.client.ItemSerializer;
import kakapo.client.model.BaseHeaderOrContent;
import kakapo.client.model.ItemDeserializeException;
import kakapo.client.model.ItemSerializeException;
import kakapo.client.model.RegularContentV1;
import kakapo.client.model.RegularHeaderV1;
import kakapo.client.model.ResponseHeaderV1;
import kakapo.client.model.UnknownItemTypeException;
import kakapo.crypto.EncryptionResult;
import kakapo.crypto.ICryptoService;
import kakapo.crypto.exception.DecryptFailedException;
import kakapo.crypto.exception.EncryptFailedException;
import kakapo.crypto.exception.SignatureVerificationFailedException;
import kakapo.util.TimeUtil;
import okhttp3.ResponseBody;
import retrofit2.Response;

@Singleton
public class ShareService {

    private static final String TAG = ShareService.class.getSimpleName();

    public static final int REQUEST_EDIT_RESPONSE_ITEM = 100;

    @Inject
    Context _context;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    PreKeyDAO _preKeyDAO;

    @Inject
    ShareDAO _shareDAO;

    @Inject
    ShareRecipientDAO _shareRecipientDAO;

    @Inject
    FriendDAO _friendDAO;

    @Inject
    ICryptoService _cryptoService;

    @Inject
    RetrofitWrapper _retrofitWrapper;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    EventBus _eventBus;

    @Inject
    public ShareService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public void showShareItemActivity(Activity activity, Long shareItemId) {
        Share shareItem = _shareDAO.find(shareItemId);
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

        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());

        // Convert the recipients into a map of GUIDs and names.
        HashMap<String, String> guidMap = new HashMap<>();
        for (ShareRecipient recipient : shareItem.getRecipients()) {

            // One of the share recipients is me. Treat it special.
            if (recipient.getGuid().compareTo(userAccount.getGuid()) != 0) {
                Friend friend = _friendDAO.find(_prefsUtil.getCurrentUserAccountId(),
                        recipient.getGuid());
                guidMap.put(friend.getGuid(), friend.getName());
            }
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
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());

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
        _shareDAO.insert(shareItem);

        // Post an event
        _eventBus.post(new ShareItemQueued());

        return shareItem.getId();
    }

    public long queueItem(long userAccountId, Set<String> sharedWithGUIDs, String title, String url, String message, Uri attachmentUri, String mimeType) {

        // Fetch the user account
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());

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

        // Create share recipient entries for all friends.
        for (String guid : sharedWithGUIDs) {
            Friend friend = _friendDAO.find(userAccountId, guid);

            ShareRecipient recipient = new ShareRecipient();
            recipient.setGuid(guid);
            recipient.setSigningPublicKey(friend.getSigningPublicKey());

            shareItem.getRecipients().add(recipient);
        }

        // Create share recipient entry for myself.
        ShareRecipient me = new ShareRecipient();
        me.setGuid(userAccount.getGuid());
        me.setSigningPublicKey(userAccount.getSigningPublicKey());
        shareItem.getRecipients().add(me);

        // Persist the share item.
        _shareDAO.insert(shareItem);

        // Post an event
        _eventBus.post(new ShareItemQueued());

        return shareItem.getId();
    }

    public void deleteQueuedItem(long itemId) {
        _shareDAO.delete(itemId);
    }

    public Disposable submitItemAsync(Class<?> eventTarget,
                                      long shareItemId,
                                      String password) {
        return Observable.fromCallable(() -> submitItem(shareItemId, password))
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
                    Share shareItem = _shareDAO.find(shareItemId);
                    _shareDAO.updateError(shareItem, _context.getResources().getString(errorMessageId));

                    // Post an event.
                    _eventBus.post(SubmitItemComplete.failure(eventTarget,
                            apiException.getErrorCode(),
                            shareItemId));
                });
    }

    private SubmitItemResponse submitItem(long shareItemId, String password) throws ApiException {

        // Fetch the share item from the data store.
        Share shareItem = _shareDAO.find(shareItemId);

        // Update the status of the share item and post an event.
        _shareDAO.updateSubmitting(shareItem);
        _eventBus.post(new SubmitItemStarted(shareItemId));

        if (shareItem.getRecipients().isEmpty()) {
            // Go fetch the recipients.
            fetchRecipients(shareItem, password);
            return submitItem(shareItemId, password);
        } else {

            // Ensure we have prekeys for all recipients.
            for (ShareRecipient recipient : shareItem.getRecipients()) {
                if (recipient.getPreKey() == null) {
                    try {
                        fetchPreKey(recipient,
                                shareItem.getUserAccount().getId(),
                                password);
                    } catch (ApiException e) {
                        // TODO: Set an error message on the share item indicating that we couldn't get a prekey for the user. Well, need to interrogate the api exception to find the proper result.
                        System.out.println("Couldn't find a prekey for: " + recipient.getGuid());
                        shareItem.setState(ShareState.Error);
//                        shareItem.setErrorMessage(_context.getResources().getString(errorMessageId));
                        _shareDAO.updateError(shareItem, "TODO: Couldn't find prekey for someone");
                    }
                }
            }

            // If the share item is in error state, we don't have all the prekeys. If it isn't, proceed.
            if (shareItem.getState() == ShareState.Error) {
                // TODO: This isn't quite right, but we'll figure it out later.
                throw new ApiException(AsyncResult.NoPreKeysAvailable);
            } else {
                return submitItem(shareItem, password);
            }
        }
    }

    private void fetchRecipients(Share shareItem, String password) throws ApiException {

        // Make HTTP call.
        FetchRecipientsResponse response = _retrofitWrapper.fetchRecipients(shareItem.getParentItemRemoteId(),
                shareItem.getUserAccount().getId(),
                password);

        // Parse the recipients in the response and update the share item with recipient data.
        for (ShareItemRecipient recipient : response.getRecipients()) {
            ShareRecipient recipientEntity = new ShareRecipient();
            recipientEntity.setGuid(recipient.getGuid());
            recipientEntity.setSigningPublicKey(recipient.getSigningPublicKey());
            recipientEntity.setItem(shareItem);
            _shareRecipientDAO.insert(recipientEntity);
        }
    }

    private void fetchPreKey(ShareRecipient recipient, Long userAccountId, String password)
            throws ApiException {

        // Go get the prekey.
        FetchPreKeyResponse fetchPreKeyResponse =
                _retrofitWrapper.fetchPreKey(recipient.getGuid(), userAccountId, password);

        // Verify the prekey.
        Key recipientPublicKey = Key.fromHexString(recipient.getSigningPublicKey());
        byte[] signedPreKeyBytes = LazySodium.toBin(fetchPreKeyResponse.getSignedPreKey());
        byte[] preKey;
        try {
            preKey = _cryptoService.verifyPreKey(signedPreKeyBytes, recipientPublicKey);
        } catch (SignatureVerificationFailedException e) {
            // TODO: NoPreKeysAvailable is not the right error here... and we may not want to throw an exception. Need to think about the error handling here.
            throw new ApiException(e, AsyncResult.NoPreKeysAvailable);
        }

        // Update the share recipient with the prekey and prekey ID.
        recipient.setPreKeyId(fetchPreKeyResponse.getPreKeyId());
        recipient.setPreKey(LazySodium.toHex(preKey));
        _shareRecipientDAO.updatePreKey(recipient.getId(), fetchPreKeyResponse.getPreKeyId(),
                LazySodium.toHex(preKey));
    }

    private SubmitItemResponse submitItem(Share shareItem, String password) throws ApiException {

        // Generate a key exchange key.
        KeyPair keyExchangeKeyPair = _cryptoService.generateKeyExchangeKeypair();

        // Generate a secret key that we will use to encrypt the data. This secret key will be
        // encrypted with the secret key between us and each party.
        Key groupSecretKey = _cryptoService.generateGroupKey();

        // Build list of share item destinations.
        List<SubmitItemDestination> destinations = new ArrayList<>();
        for (ShareRecipient recipient : shareItem.getRecipients()) {

            // Generate a shared secret key for this recipient.
            byte[] sharedSecretKey = _cryptoService.calculateSharedSecret(keyExchangeKeyPair.getSecretKey(),
                    Key.fromHexString(recipient.getPreKey()));

            // Encrypt the group secret key with the shared secret key.
            EncryptionResult groupKeyEncryptionResult;
            try {
                groupKeyEncryptionResult =
                        _cryptoService.encryptGroupKey(groupSecretKey, Key.fromBytes(sharedSecretKey));
            } catch (EncryptFailedException e) {
                // TODO: Ensure that this is right. And handled.
                throw new ApiException(e, AsyncResult.EncryptionFailed);
            }

            // Build the destination record, including the prekey id so that the recipient can
            // build the shared secret and the encrypted group key that the recipient decrypt with
            // the shared secret.
            SubmitItemDestination destination = new SubmitItemDestination();
            destination.setPreKeyId(recipient.getPreKeyId());
            destination.setUserGuid(recipient.getGuid());
            destination.setEncryptedGroupKey(LazySodium.toHex(groupKeyEncryptionResult.getCiphertext()));
            destination.setGroupKeyNonce(LazySodium.toHex(groupKeyEncryptionResult.getNonce()));
            destinations.add(destination);
        }

        // Serialise the share item.
        Pair<byte[], byte[]> serializedData;
        try {
            serializedData = shareItem.serialize(_context);
        } catch (IOException | UnknownItemTypeException | ItemSerializeException e) {
            throw new ApiException(e, AsyncResult.ItemSerializationFailed);
        }

        // Encrypt the share item header and content using the group secret key.
        EncryptionResult headerEncryptionResult;
        EncryptionResult contentEncryptionResult;
        try {

            headerEncryptionResult = _cryptoService.encryptShareData(serializedData.first,
                    groupSecretKey);
            contentEncryptionResult = _cryptoService.encryptShareData(serializedData.second,
                    groupSecretKey);
        } catch (EncryptFailedException e) {
            throw new ApiException(e, AsyncResult.EncryptionFailed);
        }

        // Build the request.
        SubmitItemRequest submitItemRequest = new SubmitItemRequest();
        submitItemRequest.setKeyExchangePublicKey(keyExchangeKeyPair.getPublicKey().getAsHexString());
        submitItemRequest.setDestinations(destinations);
        submitItemRequest.setHeaderNonce(LazySodium.toHex(headerEncryptionResult.getNonce()));
        submitItemRequest.setContentNonce(LazySodium.toHex(contentEncryptionResult.getNonce()));
        submitItemRequest.setParentRemoteItemId(shareItem.getParentItemRemoteId());

        // Make HTTP call.
        SubmitItemResponse submitItemResponse =
                _retrofitWrapper.submitItem(shareItem.getUserAccount().getId(),
                        password,
                        submitItemRequest,
                        headerEncryptionResult.getCiphertext(),
                        contentEncryptionResult.getCiphertext());

        // If everything was successful until now, delete the item from the database.
        deleteQueuedItem(shareItem.getId());

        // Return the response.
        return submitItemResponse;
    }

    public Disposable deleteItemAsync(long itemRemoteId, long userAccountId, String password) {
        return Observable.fromCallable(() -> deleteItem(itemRemoteId, userAccountId, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result ->
                                _eventBus.post(DeleteItemComplete.success(result.getItemRemoteId(),
                                        result.getUsedQuota(),
                                        result.getMaxQuota())),
                        throwable -> {
                            ApiException apiException = (ApiException) throwable;
                            _eventBus.post(DeleteItemComplete.failure(apiException.getErrorCode()));
                        });
    }

    private DeleteItemResponse deleteItem(long remoteId,
                                          long userAccountId,
                                          String password)
            throws ApiException {
        // Make HTTP call.
        return _retrofitWrapper.deleteItem(remoteId,
                userAccountId,
                password);
    }

    public Disposable fetchItemHeadersForParentAsync(Class<?> eventTarget,
                                                     long userAccountId,
                                                     String password,
                                                     Long parentItemRemoteId) {
        return fetchItemHeadersAsync(eventTarget,
                userAccountId,
                password,
                null,
                parentItemRemoteId,
                null);
    }

    public Disposable fetchItemHeaderAsync(Class<?> eventTarget,
                                           long userAccountId,
                                           String password,
                                           Long itemRemoteId) {
        return fetchItemHeadersAsync(eventTarget,
                userAccountId,
                password,
                null,
                null,
                itemRemoteId);
    }

    public Disposable fetchItemHeadersAsync(Class<?> eventTarget,
                                            long userAccountId,
                                            String password,
                                            Long lastItemRemoteId) {
        return fetchItemHeadersAsync(eventTarget,
                userAccountId,
                password,
                lastItemRemoteId,
                null,
                null);
    }

    private Disposable fetchItemHeadersAsync(Class<?> eventTarget,
                                             long userAccountId,
                                             String password,
                                             Long lastItemRemoteId,
                                             Long parentItemRemoteId,
                                             Long itemRemoteId) {
        return Observable.fromCallable(() -> fetchItemHeaders(userAccountId,
                password,
                lastItemRemoteId,
                parentItemRemoteId,
                itemRemoteId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(FetchItemHeadersComplete.success(eventTarget, result.getShareItems(), result.getRemainingItemCount())), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(FetchItemHeadersComplete.failure(eventTarget, apiException.getErrorCode()));
                });
    }

    private FetchItemHeadersResponse fetchItemHeaders(long userAccountId,
                                                      String password,
                                                      Long lastItemRemoteId,
                                                      Long parentItemRemoteId,
                                                      Long itemRemoteId) throws ApiException {

        // Make HTTP call.
        return _retrofitWrapper.fetchItemHeaders(userAccountId,
                password,
                _context.getResources().getInteger(R.integer.news_items_per_page),
                lastItemRemoteId,
                parentItemRemoteId,
                itemRemoteId);
    }

    public Disposable decryptShareItemHeaderAsync(Class<?> eventTarget,
                                                  long userAccountId,
                                                  ShareItem shareItem) {
        return Observable.fromCallable(() ->
                decryptShareItemHeader(userAccountId, shareItem))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(NewsItemDecryptComplete.success(eventTarget, result)), throwable -> {
                    throwable.printStackTrace();
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(NewsItemDecryptComplete.failure(eventTarget, shareItem.getRemoteId(), apiException.getErrorCode()));
                });
    }

    private AbstractNewsListItem decryptShareItemHeader(long userAccountId,
                                                        ShareItem shareItem) throws ApiException {

        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());

        // First fetch the prekey specified in the share item.
        PreKey preKey = _preKeyDAO.find(userAccountId, shareItem.getPreKeyId());
        Key secretPreKey = Key.fromHexString(preKey.getSecretKey());

        // TODO: What if it's not found?

        // Use the prekey and the key exchange public key to get our shared secret.
        byte[] sharedSecretKey = _cryptoService.calculateSharedSecret(secretPreKey,
                Key.fromHexString(shareItem.getKeyExchangePublicKey()));

        // Use the shared secret to decrypt the group key
        byte[] groupKeyNonceBytes = LazySodium.toBin(shareItem.getGroupKeyNonce());
        byte[] encryptedGroupKeyBytes = LazySodium.toBin(shareItem.getEncryptedGroupKey());
        byte[] groupKeyBytes;
        try {
            groupKeyBytes = _cryptoService.decryptGroupKey(Key.fromBytes(sharedSecretKey),
                    groupKeyNonceBytes,
                    encryptedGroupKeyBytes);
        } catch (DecryptFailedException e) {
            throw new ApiException(e, AsyncResult.DecryptionFailed);
        }

        // Use the group key to decrypt the data.
        byte[] headerNonce = LazySodium.toBin(shareItem.getHeaderNonce());
        byte[] encryptedHeaderBytes = LazySodium.toBin(shareItem.getEncryptedHeader());
        byte[] headerData;
        try {
            headerData = _cryptoService.decryptShareData(encryptedHeaderBytes,
                    headerNonce,
                    Key.fromBytes(groupKeyBytes));
        } catch (DecryptFailedException e) {
            throw new ApiException(e, AsyncResult.DecryptionFailed);
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
        newsItem.setChildCount(shareItem.getChildCount());

        // Set the state.
        if (shareItem.isMarkedAsDeleted()) {
            newsItem.setState(NewsListItemState.Deleted);
        } else {
            newsItem.setState(NewsListItemState.Decrypted);
        }

        return newsItem;
    }

    public Disposable streamItemContentAsync(long userAccountId, String password, long itemRemoteId) {
        return Observable.create((ObservableOnSubscribe<EncryptedDataWithMetadata>) emitter -> {
            EncryptedDataWithMetadata encryptedDataWithMetadata =
                    streamItemContent(emitter, userAccountId, password, itemRemoteId);
            emitter.onNext(encryptedDataWithMetadata);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        _eventBus.post(ContentStreamComplete.success(itemRemoteId,
                                result.getEncryptedData(),
                                result.getPreKeyId(),
                                result.getKeyExchangePublicKey(),
                                result.getEncryptedGroupKey(),
                                result.getNonce(),
                                result.getContentNonce()));
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(ContentStreamComplete.failure(apiException.getErrorCode()));
                });
    }

    private EncryptedDataWithMetadata streamItemContent(ObservableEmitter<EncryptedDataWithMetadata> emitter,
                                                        long userAccountId,
                                                        String password,
                                                        long itemRemoteId)
            throws ApiException {

        // Make HTTP call. An exception will be thrown if this fails, so we can proceed afterwards
        // as if things are successful.
        Response<ResponseBody> response = _retrofitWrapper.streamItemContent(itemRemoteId,
                userAccountId,
                password);

        // Collect data from the header for decrypting.

        // The prekey and key exchange public key are used to arrive at a shared secret between
        // us and the encryptor.
        String preKeyIdString = response.headers().get("Kakapo-Pre-Key-ID");
        Long preKeyId = null;
        if (preKeyIdString != null) {
            preKeyId = Long.valueOf(preKeyIdString);
        }
        String keyExchangePublicKey = response.headers().get("Kakapo-Key-Exchange-Public-Key");

        // The group key is encrypted using the shared secret and nonce.
        String encryptedGroupKey = response.headers().get("Kakapo-Encrypted-Group-Key");
        String nonce = response.headers().get("Kakapo-Nonce");

        // The content is encrypted using the group key and content nonce.
        String contentNonce = response.headers().get("Kakapo-Content-Nonce");

        // Save the data while reporting progress.
        ResponseBody responseBody = response.body();
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
            return new EncryptedDataWithMetadata(encryptedContentStream.toByteArray(),
                    preKeyId,
                    keyExchangePublicKey,
                    encryptedGroupKey,
                    nonce,
                    contentNonce);
        } catch (IOException e) {
            throw new ApiException(AsyncResult.ContentStreamFailed);
        }
    }

    /**
     * Decrypt and deserialize the attachment from the encrypted content. Returns immediately and
     * posts an {@link AttachmentDecryptComplete} event.
     *
     * @param userAccountId
     * @param encryptedContent
     */
    public Disposable decryptAttachmentAsync(long userAccountId,
                                             long itemRemoteId,
                                             byte[] encryptedContent,
                                             long preKeyId,
                                             String keyExchangePublicKeyString,
                                             String nonceString,
                                             String encryptedGroupKeyString,
                                             String contentNonceString) {
        return Observable.fromCallable(() ->
                decryptAttachment(userAccountId,
                        itemRemoteId,
                        encryptedContent,
                        preKeyId,
                        keyExchangePublicKeyString,
                        nonceString,
                        encryptedGroupKeyString,
                        contentNonceString))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(AttachmentDecryptComplete.success(result)), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AttachmentDecryptComplete.failure(apiException.getErrorCode()));
                });
    }

    private byte[] decryptAttachment(long userAccountId,
                                     long itemRemoteId,
                                     byte[] encryptedContent,
                                     long preKeyId,
                                     String keyExchangePublicKeyString,
                                     String groupKeyNonce,
                                     String encryptedGroupKeyString,
                                     String contentNonceString)
            throws ApiException {

        Key keyExchangePublicKey = Key.fromHexString(keyExchangePublicKeyString);

        // TODO: Code here very similar to elsewhere. Possibly in this file.

        // Fetch the prekey specified in the share item.
        PreKey preKey = _preKeyDAO.find(userAccountId, preKeyId);
        Key secretPreKey = Key.fromHexString(preKey.getSecretKey());

        // TODO: What if it's not found?

        // Use the prekey and the key exchange public key to get our shared secret.
        byte[] sharedSecretKey = _cryptoService.calculateSharedSecret(secretPreKey,
                keyExchangePublicKey);

        // Use the shared secret to decrypt the group key
        byte[] groupKeyNonceBytes = LazySodium.toBin(groupKeyNonce);
        byte[] encryptedGroupKeyBytes = LazySodium.toBin(encryptedGroupKeyString);
        byte[] groupKeyBytes;
        try {
            groupKeyBytes = _cryptoService.decryptGroupKey(Key.fromBytes(sharedSecretKey),
                    groupKeyNonceBytes,
                    encryptedGroupKeyBytes);
        } catch (DecryptFailedException e) {
            throw new ApiException(e, AsyncResult.DecryptionFailed);
        }

        // Use the group key to decrypt the data.
        byte[] contentNonce = LazySodium.toBin(contentNonceString);
        byte[] contentData;
        try {
            contentData = _cryptoService.decryptShareData(encryptedContent,
                    contentNonce,
                    Key.fromBytes(groupKeyBytes));
        } catch (DecryptFailedException e) {
            throw new ApiException(e, AsyncResult.DecryptionFailed);
        }

        // Deserialize content
        BaseHeaderOrContent content;
        try {
            content = new ItemSerializer().deserialize(contentData);
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
