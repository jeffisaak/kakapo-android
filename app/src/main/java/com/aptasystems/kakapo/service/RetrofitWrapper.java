package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;

import javax.inject.Inject;
import javax.inject.Singleton;

import kakapo.api.CustomHttpStatusCode;
import kakapo.api.request.AuthenticateRequest;
import kakapo.api.request.DeleteAccountRequest;
import kakapo.api.request.DeleteItemRequest;
import kakapo.api.request.DownloadAccountRequest;
import kakapo.api.request.FetchItemHeadersRequest;
import kakapo.api.request.FetchPublicKeyRequest;
import kakapo.api.request.FetchRecipientsRequest;
import kakapo.api.request.QuotaRequest;
import kakapo.api.request.ServerConfigRequest;
import kakapo.api.request.SignUpRequest;
import kakapo.api.request.StreamContentRequest;
import kakapo.api.request.SubmitItemRequest;
import kakapo.api.request.UploadAccountRequest;
import kakapo.api.response.DeleteItemResponse;
import kakapo.api.response.DownloadAccountResponse;
import kakapo.api.response.FetchItemHeadersResponse;
import kakapo.api.response.FetchPublicKeyResponse;
import kakapo.api.response.FetchRecipientsResponse;
import kakapo.api.response.QuotaResponse;
import kakapo.api.response.RequestGuidResponse;
import kakapo.api.response.ServerConfigResponse;
import kakapo.api.response.SubmitItemResponse;
import kakapo.api.response.UploadAccountResponse;
import kakapo.client.retrofit.RetrofitService;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

@Singleton
public class RetrofitWrapper {

    private static final String TAG = RetrofitWrapper.class.getSimpleName();

    private RetrofitService _retrofitService;

    @Inject
    public RetrofitWrapper(RetrofitService retrofitService) {
        _retrofitService = retrofitService;
    }

    public RequestGuidResponse requestGuid() throws ApiException {

        Response<RequestGuidResponse> response = null;
        try {
            response = _retrofitService.requestGuid().execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public void createAccount(SignUpRequest request) throws ApiException {

        try {
            Response response = _retrofitService.createAccount(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
    }

    public void deleteAccount(DeleteAccountRequest request) throws ApiException {

        try {
            Response response = _retrofitService.deleteAccount(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
    }

    public void authenticate(AuthenticateRequest request) throws ApiException {

        try {
            Response response = _retrofitService.authenticate(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
    }

    public FetchPublicKeyResponse fetchPublicKey(FetchPublicKeyRequest request) throws ApiException {

        Response<FetchPublicKeyResponse> response = null;
        try {
            response = _retrofitService.fetchPublicKey(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public QuotaResponse fetchQuota(QuotaRequest request) throws ApiException {

        Response<QuotaResponse> response = null;
        try {
            response = _retrofitService.fetchQuota(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public UploadAccountResponse uploadAccount(UploadAccountRequest request) throws ApiException {

        Response<UploadAccountResponse> response = null;
        try {
            response = _retrofitService.uploadAccount(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public DownloadAccountResponse downloadAccount(DownloadAccountRequest request)
            throws ApiException {

        Response<DownloadAccountResponse> response = null;
        try {
            response = _retrofitService.downloadAccount(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public ServerConfigResponse getServerConfig(ServerConfigRequest request) throws ApiException {

        Response<ServerConfigResponse> response = null;
        try {
            response = _retrofitService.serverConfig(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public SubmitItemResponse submitItem(SubmitItemRequest request,
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
            response = _retrofitService.submitItem(jsonPart, headerPart, contentPart).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public DeleteItemResponse deleteItem(DeleteItemRequest request) throws ApiException {

        Response<DeleteItemResponse> response = null;
        try {
            response = _retrofitService.deleteItem(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public FetchItemHeadersResponse fetchItemHeaders(FetchItemHeadersRequest request)
            throws ApiException {

        Response<FetchItemHeadersResponse> response = null;
        try {
            response = _retrofitService.fetchItemHeaders(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public ResponseBody streamItemContent(StreamContentRequest request) throws ApiException {

        Response<ResponseBody> response = null;
        try {
            response = _retrofitService.streamItemContent(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
    }

    public FetchRecipientsResponse fetchRecipients(FetchRecipientsRequest request)
            throws ApiException {

        Response<FetchRecipientsResponse> response = null;
        try {
            response = _retrofitService.fetchRecipients(request).execute();
            throwHttpExceptionIfNecessary(response);
        } catch (IOException e) {
            wrap(e);
        }
        return response.body();
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

    private void throwHttpExceptionIfNecessary(Response<?> httpResponse) throws ApiException {
System.out.println(httpResponse.code());
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
            default:
                throw new ApiException(AsyncResult.OtherHttpError);
        }
    }

}
