package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.exception.BadRequestException;
import com.aptasystems.kakapo.exception.BackupVersionNumberConflictException;
import com.aptasystems.kakapo.exception.InvalidKeyLengthException;
import com.aptasystems.kakapo.exception.NoPreKeysAvailableException;
import com.aptasystems.kakapo.exception.NotFoundException;
import com.aptasystems.kakapo.exception.OtherHttpErrorException;
import com.aptasystems.kakapo.exception.PayloadTooLargeException;
import com.aptasystems.kakapo.exception.QuotaExceededException;
import com.aptasystems.kakapo.exception.RetrofitIOException;
import com.aptasystems.kakapo.exception.ServerUnavailableException;
import com.aptasystems.kakapo.exception.TooManyRequestsException;
import com.aptasystems.kakapo.exception.UnauthorizedException;
import com.aptasystems.kakapo.worker.UploadPreKeysWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.ConnectException;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
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

    private KakapoApplication _application;

    @Inject
    RetrofitService _retrofitService;

    @Inject
    EventBus _eventBus;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    public RetrofitWrapper(KakapoApplication application) {
        _application = application;
        application.getKakapoComponent().inject(this);
    }

    SignUpResponse createAccount(SignUpRequest request)
            throws BadRequestException,
            TooManyRequestsException,
            InvalidKeyLengthException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<SignUpResponse> response = null;
        try {
            response = _retrofitService.createAccount(request).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 429:
                    throw new TooManyRequestsException();
                case CustomHttpStatusCode.INSUFFICIENT_KEY_LENGTH:
                    throw new InvalidKeyLengthException();
                default:
                    throw new OtherHttpErrorException();
            }

        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    void authenticate(Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.authenticate(userAccount.getGuid(),
                    userAccount.getGuid(), userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }

        } catch (IOException e) {
            wrap(e);
        }

        checkPreKeysRemainingHeader(response, userAccountId, password);
    }

    UploadPreKeysResponse uploadPreKeys(String userGuid,
                                        String apiKey,
                                        UploadPreKeysRequest request)
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            InvalidKeyLengthException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<UploadPreKeysResponse> response = null;
        try {
            response = _retrofitService.uploadPreKeys(userGuid,
                    userGuid,
                    apiKey,
                    request).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                case CustomHttpStatusCode.INSUFFICIENT_KEY_LENGTH:
                    throw new InvalidKeyLengthException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    void deleteAccount(Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.deleteAccount(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }

        checkPreKeysRemainingHeader(response, userAccountId, password);
    }

    FetchPreKeyResponse fetchPreKey(String targetUserGuid,
                                    Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            TooManyRequestsException,
            NoPreKeysAvailableException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<FetchPreKeyResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchPreKey(targetUserGuid,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 404:
                    throw new NotFoundException();
                case 429:
                    throw new TooManyRequestsException();
                case CustomHttpStatusCode.NO_PREKEYS_AVAILABLE:
                    throw new NoPreKeysAvailableException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    QuotaResponse fetchQuota(Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<QuotaResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchQuota(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }

        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    FetchPublicKeyResponse fetchPublicKey(String targetUserGuid,
                                          Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<FetchPublicKeyResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchPublicKey(targetUserGuid,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 404:
                    throw new NotFoundException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
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
                                              byte[] encryptedAccountData)
            throws BadRequestException,
            UnauthorizedException,
            BackupVersionNumberConflictException,
            PayloadTooLargeException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

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
            throw new BadRequestException();
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

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 409:
                    throw new BackupVersionNumberConflictException();
                case 413:
                    throw new PayloadTooLargeException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    GetBackupVersionResponse getBackupVersion(Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<GetBackupVersionResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.getAccountBackupVersion(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    Response<ResponseBody> streamAccountBackup(Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<ResponseBody> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.streamAccountBackup(userAccount.getGuid(),
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 404:
                    throw new NotFoundException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response;
    }

    void blacklist(String guidToBlacklist,
                   Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.blacklist(userAccount.getGuid(),
                    guidToBlacklist,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 404:
                    throw new NotFoundException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
    }

    public ServerConfigResponse getServerConfig(Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<ServerConfigResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.serverConfig(userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    SubmitItemResponse submitItem(Long userAccountId, String password,
                                  SubmitItemRequest request,
                                  byte[] encryptedHeader,
                                  byte[] encryptedContent)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            PayloadTooLargeException,
            TooManyRequestsException,
            QuotaExceededException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        // Convert our request object to a JSON string using Jackson.
        ObjectMapper mapper = new ObjectMapper();
        String requestJson;
        try {
            requestJson = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // Shouldn't happen...
            throw new BadRequestException();
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

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 404:
                    throw new NotFoundException();
                case 413:
                    throw new PayloadTooLargeException();
                case 429:
                    throw new TooManyRequestsException();
                case CustomHttpStatusCode.QUOTA_EXCEEDED:
                    throw new QuotaExceededException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
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
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<FetchItemHeadersResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchItemHeaders(userAccount.getGuid(),
                    userAccount.getApiKey(),
                    itemCount,
                    lastItemRemoteId,
                    parentItemRemoteId,
                    itemRemoteId).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    FetchRecipientsResponse fetchRecipients(Long itemRemoteId,
                                            Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<FetchRecipientsResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.fetchRecipients(itemRemoteId,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    DeleteItemResponse deleteItem(Long itemRemoteId,
                                  Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<DeleteItemResponse> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.deleteItem(itemRemoteId,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 404:
                    throw new NotFoundException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response.body();
    }

    Response<ResponseBody> streamItemContent(Long itemRemoteId,
                                             Long userAccountId, String password)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            TooManyRequestsException,
            OtherHttpErrorException,
            ServerUnavailableException,
            RetrofitIOException {

        Response<ResponseBody> response = null;
        try {
            UserAccount userAccount = _userAccountDAO.find(userAccountId);
            response = _retrofitService.streamItemContent(itemRemoteId,
                    userAccount.getGuid(),
                    userAccount.getApiKey()).execute();

            switch (response.code()) {
                case 200:
                    break;
                case 400:
                    throw new BadRequestException();
                case 401:
                    throw new UnauthorizedException();
                case 404:
                    throw new NotFoundException();
                case 429:
                    throw new TooManyRequestsException();
                default:
                    throw new OtherHttpErrorException();
            }
        } catch (IOException e) {
            wrap(e);
        }
        checkPreKeysRemainingHeader(response, userAccountId, password);
        return response;
    }

    private void wrap(IOException e) throws ServerUnavailableException, RetrofitIOException {
        // Check the IOException thrown by Retrofit to see if it
        // indicates that we couldn't connect to the server or something else. In either case, we
        // will throw a new, more specific exception.
        if (e instanceof ConnectException &&
                e.getMessage().contains("Failed to connect to")) {
            throw new ServerUnavailableException();
        } else {
            throw new RetrofitIOException();
        }
    }

    private void checkPreKeysRemainingHeader(Response<?> response,
                                             long userAccountId,
                                             String password) {

        // Get the number of pre keys remaining from the header. If it's less than a certain
        // threshold, fire off a worker to generate and upload some new prekeys. The worker should
        // only start once (hence the ExistingWorkPolicy.KEEP below).
        String preKeysRemainingString = response.headers().get("Kakapo-Pre-Keys-Remaining");
        if (preKeysRemainingString != null) {
            long preKeysRemaining = Long.parseLong(preKeysRemainingString);
            if (preKeysRemaining < _application.getResources().getInteger(R.integer.minimum_pre_key_threshold)) {
                Data uploadPreKeysData = new Data.Builder()
                        .putLong(UploadPreKeysWorker.KEY_USER_ACCOUNT_ID, userAccountId)
                        .putString(UploadPreKeysWorker.KEY_PASSWORD, password)
                        .build();
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(UploadPreKeysWorker.class)
                        .setInputData(uploadPreKeysData)
                        .build();
                WorkManager.getInstance(_application).enqueueUniqueWork("uploadPreKeys",
                        ExistingWorkPolicy.KEEP,
                        workRequest);
            }
        }
    }

    private void throwHttpExceptionIfNecessary(Response<?> httpResponse)
            throws BadRequestException,
            UnauthorizedException,
            NotFoundException,
            BackupVersionNumberConflictException,
            PayloadTooLargeException,
            TooManyRequestsException,
            QuotaExceededException,
            InvalidKeyLengthException,
            NoPreKeysAvailableException,
            OtherHttpErrorException {

        if (httpResponse.isSuccessful()) {
            return;
        }

        switch (httpResponse.code()) {
            case 400:
                throw new BadRequestException();
            case 401:
                throw new UnauthorizedException();
            case 404:
                throw new NotFoundException();
            case 409:
                throw new BackupVersionNumberConflictException();
            case 413:
                throw new PayloadTooLargeException();
            case 429:
                throw new TooManyRequestsException();
            case CustomHttpStatusCode.QUOTA_EXCEEDED:
                throw new QuotaExceededException();
            case CustomHttpStatusCode.INSUFFICIENT_KEY_LENGTH:
                throw new InvalidKeyLengthException();
            case CustomHttpStatusCode.NO_PREKEYS_AVAILABLE:
                throw new NoPreKeysAvailableException();
            default:
                throw new OtherHttpErrorException();
        }
    }

}
