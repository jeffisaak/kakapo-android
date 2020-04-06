package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.UploadPreKeysRequested;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.ConnectException;

import javax.inject.Inject;
import javax.inject.Singleton;

import kakapo.api.CustomHttpStatusCode;
import kakapo.api.request.BackupAccountRequest;
import kakapo.api.request.SignUpRequest;
import kakapo.api.request.SubmitItemRequest;
import kakapo.api.request.UploadPreKeysRequest;
import kakapo.api.response.BackupAccountResponse;
import kakapo.api.response.DeleteItemResponse;
import kakapo.api.response.FetchItemHeadersResponse;
import kakapo.api.response.FetchPreKeyResponse;
import kakapo.api.response.FetchPublicKeyResponse;
import kakapo.api.response.FetchRecipientsResponse;
import kakapo.api.response.GetBackupVersionResponse;
import kakapo.api.response.QuotaResponse;
import kakapo.api.response.ServerConfigResponse;
import kakapo.api.response.SignUpResponse;
import kakapo.api.response.SubmitItemResponse;
import kakapo.api.response.UploadPreKeysResponse;
import kakapo.client.retrofit.RetrofitService;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

@Singleton
public class RetrofitWrapper {

    private static final String TAG = RetrofitWrapper.class.getSimpleName();

    @Inject
    RetrofitService _retrofitService;

    @Inject
    EventBus _eventBus;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public RetrofitWrapper(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    SignUpResponse createAccount(SignUpRequest request) throws ApiException {

        Response<SignUpResponse> response = null;
        try {
            response = _retrofitService.createAccount(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    void authenticate(Long userAccountId, String password) throws ApiException {

        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            Response response = _retrofitService.authenticate(userAccount.getGuid(),
                    userAccount.getGuid(), userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
            checkPreKeysRemainingHeader(response, userAccountId, password);
        } catch (IOException e) {
            wrap(e);
        }
    }

    UploadPreKeysResponse uploadPreKeys(String userGuid,
                                        String apiKey,
                                        UploadPreKeysRequest request)
            throws ApiException {

        Response<UploadPreKeysResponse> response = null;
        try {
            response = _retrofitService.uploadPreKeys(userGuid,
                    userGuid,
                    apiKey,
                    request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    void deleteAccount(Long userAccountId, String password) throws ApiException {
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            Response response = _retrofitService.deleteAccount(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
            checkPreKeysRemainingHeader(response, userAccountId, password);
        } catch (IOException e) {
            wrap(e);
        }
    }

    FetchPreKeyResponse fetchPreKey(String targetUserGuid,
                                    Long userAccountId, String password)
            throws ApiException {

        Response<FetchPreKeyResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchPreKey(targetUserGuid,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    QuotaResponse fetchQuota(Long userAccountId, String password) throws ApiException {

        Response<QuotaResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchQuota(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    FetchPublicKeyResponse fetchPublicKey(String targetUserGuid,
                                          Long userAccountId, String password)
            throws ApiException {

        Response<FetchPublicKeyResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchPublicKey(targetUserGuid,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    BackupAccountResponse uploadAccountBackup(Long userAccountId,
                                              String password,
                                              Long backupVersionToUpdate,
                                              String nonce,
                                              byte[] encryptedAccountData) throws ApiException {

        // Build a request from the parameters.
        BackupAccountRequest request = new BackupAccountRequest();
        request.setBackupVersionToUpdate(backupVersionToUpdate);
        request.setNonce(nonce);

        // Convert our request object to a JSON string using Jackson.
        ObjectMapper mapper = new ObjectMapper();
        String requestJson;
        try {
            requestJson = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // Shouldn't happen...
            throw new ApiException(e, AsyncResult.BadRequest);
        }

        // Build our multipart stuff.

        RequestBody json = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"),
                requestJson);
        MultipartBody.Part jsonPart = MultipartBody.Part.createFormData("json", "json", json);

        RequestBody accountData = RequestBody.create(MediaType.parse("application/octet-stream"),
                encryptedAccountData);
        MultipartBody.Part accountDataPart =
                MultipartBody.Part.createFormData("data", "data", accountData);

        // Make the request and return the response.
        Response<BackupAccountResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.uploadAccountBackup(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey(),
                    jsonPart,
                    accountDataPart)
                    .execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    GetBackupVersionResponse getBackupVersion(Long userAccountId, String password)
            throws ApiException {

        Response<GetBackupVersionResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.getAccountBackupVersion(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    Response<ResponseBody> streamAccountBackup(Long userAccountId, String password)
            throws ApiException {

        Response<ResponseBody> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.streamAccountBackup(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response;
    }

    void blacklist(String guidToBlacklist,
                   Long userAccountId, String password) throws ApiException {
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            Response response = _retrofitService.blacklist(userAccount.getGuid(),
                    guidToBlacklist,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
            checkPreKeysRemainingHeader(response, userAccountId, password);
        } catch (IOException e) {
            wrap(e);
        }
    }

    public ServerConfigResponse getServerConfig(Long userAccountId, String password)
            throws ApiException {

        Response<ServerConfigResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.serverConfig(userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    SubmitItemResponse submitItem(Long userAccountId, String password,
                                  SubmitItemRequest request,
                                  byte[] encryptedHeader,
                                  byte[] encryptedContent) throws ApiException {

        // Convert our request object to a JSON string using Jackson.
        ObjectMapper mapper = new ObjectMapper();
        String requestJson;
        try {
            requestJson = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // Shouldn't happen...
            throw new ApiException(e, AsyncResult.BadRequest);
        }

        // Build our multipart stuff.

        // First build some request bodies.
        RequestBody json = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"),
                requestJson);
        RequestBody header = RequestBody.create(MediaType.parse("application/octet-stream"),
                encryptedHeader);
        RequestBody content = RequestBody.create(MediaType.parse("application/octet-stream"),
                encryptedContent);

        // Build our parts from the request bodies.
        MultipartBody.Part jsonPart = MultipartBody.Part.createFormData("json", "json", json);
        MultipartBody.Part headerPart = MultipartBody.Part.createFormData("header", "header", header);
        MultipartBody.Part contentPart = MultipartBody.Part.createFormData("content", "content", content);

        Response<SubmitItemResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.submitItem(userAccount.getGuid(),
                    userAccount.getApiKey(),
                    jsonPart,
                    headerPart,
                    contentPart)
                    .execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            e.printStackTrace();
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    FetchItemHeadersResponse fetchItemHeaders(Long userAccountId, String password,
                                              Integer itemCount,
                                              Long lastItemRemoteId,
                                              Long parentItemRemoteId,
                                              Long itemRemoteId)
            throws ApiException {

        Response<FetchItemHeadersResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchItemHeaders(userAccount.getGuid(),
                    userAccount.getApiKey(),
                    itemCount,
                    lastItemRemoteId,
                    parentItemRemoteId,
                    itemRemoteId).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    FetchRecipientsResponse fetchRecipients(Long itemRemoteId,
                                            Long userAccountId, String password)
            throws ApiException {

        Response<FetchRecipientsResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchRecipients(itemRemoteId,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    DeleteItemResponse deleteItem(Long itemRemoteId,
                                  Long userAccountId, String password) throws ApiException {

        Response<DeleteItemResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.deleteItem(itemRemoteId,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    Response<ResponseBody> streamItemContent(Long itemRemoteId,
                                             Long userAccountId, String password)
            throws ApiException {

        Response<ResponseBody> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.streamItemContent(itemRemoteId,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response;
    }

    private void wrap(IOException e) throws ApiException {
        // Check the IOException thrown by Retrofit to see if it
        // indicates that we couldn't connect to the server or something else. In either case, we
        // will throw a new, more specific exception.
        if (e instanceof ConnectException &&
                e.getMessage().contains("Failed to connect to")) {
            throw new ApiException(e, AsyncResult.ServerUnavailable);
        } else {
            throw new ApiException(e, AsyncResult.RetrofitIOException);
        }
    }

    private void checkPreKeysRemainingHeader(Response<?> response,
                                             long userAccountId,
                                             String password) {

        String preKeysRemainingString = response.headers().get("Kakapo-Pre-Keys-Remaining");
        if (preKeysRemainingString != null) {
            long preKeysRemaining = Long.parseLong(preKeysRemainingString);
            // TODO: Magic number.
            if (preKeysRemaining < 100) {
                _eventBus.post(new UploadPreKeysRequested(userAccountId, password));
            }
        }
    }

    private void throwHttpExceptionIfNecessary(Response<?> httpResponse) throws ApiException {

        if (httpResponse.isSuccessful()) {
            return;
        }

        switch (httpResponse.code()) {
            case 400:
                throw new ApiException(AsyncResult.BadRequest);
            case 401:
                throw new ApiException(AsyncResult.Unauthorized);
            case 404:
                throw new ApiException(AsyncResult.NotFound);
            case 413:
                throw new ApiException(AsyncResult.PayloadTooLarge);
            case 429:
                throw new ApiException(AsyncResult.TooManyRequests);
            case CustomHttpStatusCode.QUOTA_EXCEEDED:
                throw new ApiException(AsyncResult.QuotaExceeded);
            case CustomHttpStatusCode.INSUFFICIENT_KEY_LENGTH:
                throw new ApiException(AsyncResult.InsufficientKeyLength);
            case CustomHttpStatusCode.NO_PREKEYS_AVAILABLE:
                throw new ApiException(AsyncResult.NoPreKeysAvailable);
            default:
                throw new ApiException(AsyncResult.OtherHttpError);
        }
    }

}
