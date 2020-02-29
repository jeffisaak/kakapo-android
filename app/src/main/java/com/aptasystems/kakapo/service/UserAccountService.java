package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountCreationComplete;
import com.aptasystems.kakapo.event.AccountDeletionComplete;
import com.aptasystems.kakapo.event.AuthenticationComplete;
import com.aptasystems.kakapo.event.BlacklistAuthorComplete;
import com.aptasystems.kakapo.event.QuotaComplete;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.util.ColourUtil;

import org.greenrobot.eventbus.EventBus;

import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.request.AuthenticateRequest;
import kakapo.api.request.BlacklistRequest;
import kakapo.api.request.DeleteAccountRequest;
import kakapo.api.request.QuotaRequest;
import kakapo.api.request.SignUpRequest;
import kakapo.api.response.QuotaResponse;
import kakapo.api.response.RequestGuidResponse;
import kakapo.crypto.KeyPair;
import kakapo.crypto.PGPEncryptionService;
import kakapo.crypto.exception.KeyGenerationException;
import kakapo.crypto.exception.KeyRingSerializationException;
import kakapo.crypto.exception.SignMessageException;
import kakapo.util.HashUtil;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.generation.type.length.RsaLength;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;

@Singleton
public class UserAccountService {

    private static final String TAG = UserAccountService.class.getSimpleName();

    private PGPEncryptionService _pgpEncryptionService;
    private RetrofitWrapper _retrofitWrapper;
    private ColourUtil _colourUtil;
    private EntityDataStore<Persistable> _entityStore;
    private EventBus _eventBus;

    @Inject
    public UserAccountService(PGPEncryptionService pgpEncryptionService,
                              RetrofitWrapper retrofitWrapper,
                              ColourUtil colourUtil,
                              EntityDataStore<Persistable> entityStore,
                              EventBus eventBus) {
        _pgpEncryptionService = pgpEncryptionService;
        _retrofitWrapper = retrofitWrapper;
        _colourUtil = colourUtil;
        _entityStore = entityStore;
        _eventBus = eventBus;
    }

    /**
     * Check the hashed password by attempting to sign a message using it and the secret key. If the
     * check passes, it merely returns. If it fails, an exception is thrown.
     *
     * @param guid
     * @param hashedPassword
     * @param secretKeyRings
     * @param publicKeyRings
     * @throws SignMessageException
     */
    public void checkPassword(String guid, String hashedPassword, byte[] secretKeyRings, byte[] publicKeyRings)
            throws SignMessageException {

        // Attempt to sign a quick message using the private key and entered password.
        // This will tell us if the entered password is correct.
        byte[] randomData = new byte[512];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomData);

        KeyPair keyPair = new KeyPair(secretKeyRings, publicKeyRings);
        _pgpEncryptionService.sign(randomData,
                keyPair,
                guid,
                hashedPassword);
    }

    public Disposable createNewAccountAsync(String name, String password, RsaLength rsaLength) {
        return Observable.fromCallable(() -> createNewAccount(name, password, rsaLength))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(AccountCreationComplete.success(result)), throwable -> {
                    throwable.printStackTrace();
                    if (throwable.getCause() != null) {
                        throwable.getCause().printStackTrace();
                    }
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AccountCreationComplete.failure(apiException.getErrorCode()));
                });
    }

    private String createNewAccount(String name, String password, RsaLength keySize) throws ApiException {

        // Hash the password.
        String hashedPassword = HashUtil.sha256ToString(password);

        // Request a GUID.
        RequestGuidResponse requestGuidResponse = _retrofitWrapper.requestGuid();

        // The response contains our GUID. Generate keys and use them and our GUID to finish
        // account creation.

        // Generate keys.
        KeyringConfig keyringConfig;
        try {
            keyringConfig = _pgpEncryptionService.generateKeyRings(keySize,
                    requestGuidResponse.getGuid(),
                    hashedPassword);
        } catch (KeyGenerationException e) {
            throw new ApiException(e, AsyncResult.KeyGenerationFailed);
        }

        // Serialize the key rings so we can persist them.
        KeyPair keyPair;
        try {
            keyPair = _pgpEncryptionService.serializeKeyRings(keyringConfig);
        } catch (KeyRingSerializationException e) {
            throw new ApiException(e, AsyncResult.KeySerializationFailed);
        }

        // Build the request.
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setGuid(requestGuidResponse.getGuid());
        signUpRequest.setPublicKeys(keyPair.getPublicKey());

        // Make HTTP call.
        _retrofitWrapper.createAccount(signUpRequest);

        // Persist the user account on the device.
        UserAccount userAccount = new UserAccount();
        userAccount.setName(name);
        userAccount.setGuid(requestGuidResponse.getGuid());
        userAccount.setColour(_colourUtil.randomColour());
        userAccount.setPublicKeyRings(keyPair.getPublicKey());
        userAccount.setSecretKeyRings(keyPair.getSecretKey());
        _entityStore.insert(userAccount);

        // Return the GUID.
        return requestGuidResponse.getGuid();
    }

    public Disposable deleteAccountFromServerAsync(UserAccount userAccount, String hashedPassword) {

        return Completable.fromCallable(() -> deleteAccountFromServer(userAccount, hashedPassword))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> _eventBus.post(AccountDeletionComplete.success(true)), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AccountDeletionComplete.failure(apiException.getErrorCode()));
                });
    }

    private Void deleteAccountFromServer(UserAccount userAccount, String hashedPassword) throws ApiException {

        // Build the request.
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setGuid(userAccount.getGuid());

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
        _retrofitWrapper.deleteAccount(request);

        // Delete the account from the device.
        deleteAccountFromDevice(userAccount);

        return null;
    }

    public void deleteAccountFromDevice(UserAccount userAccount) {
        // Delete the user account - everything else will cascade.
        _entityStore.delete(userAccount);
    }

    public Disposable blacklistAuthorAsync(UserAccount userAccount, String hashedPassword, String targetGuid) {
        return Completable.fromCallable(() -> blacklistAuthor(userAccount, hashedPassword, targetGuid))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> _eventBus.post(BlacklistAuthorComplete.success(targetGuid)), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(BlacklistAuthorComplete.failure(apiException.getErrorCode()));
                });
    }

    private Void blacklistAuthor(UserAccount userAccount, String hashedPassword, String targetGuid)
            throws ApiException {

        // Build the request.
        BlacklistRequest request = new BlacklistRequest();
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
        _retrofitWrapper.blacklist(request);

        return null;
    }

    public Disposable authenticateAsync(UserAccount userAccount, String hashedPassword) {
        return Completable.fromCallable(() -> authenticate(userAccount, hashedPassword))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    // Post result.
                    _eventBus.post(AuthenticationComplete.success(userAccount.getId(), hashedPassword));
                }, throwable -> {
                    throwable.printStackTrace();
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AuthenticationComplete.failure(apiException.getErrorCode()));
                });
    }

    private Void authenticate(UserAccount userAccount, String hashedPassword)
            throws ApiException {

        // Build the request.
        AuthenticateRequest request = new AuthenticateRequest();
        request.setGuid(userAccount.getGuid());

        // Encrypt and sign the request.
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
        _retrofitWrapper.authenticate(request);

        return null;
    }

    public Disposable getQuotaAsync(UserAccount userAccount, String hashedPassword) {
        return Observable.fromCallable(() -> getQuota(userAccount, hashedPassword))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(QuotaComplete.success(result.getUsedQuota(), result.getMaxQuota())), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(QuotaComplete.failure(apiException.getErrorCode()));
                });
    }

    private QuotaResponse getQuota(UserAccount userAccount, String hashedPassword) throws ApiException {

        // Build the request.
        QuotaRequest request = new QuotaRequest();
        request.setGuid(userAccount.getGuid());

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
        return _retrofitWrapper.fetchQuota(request);
    }
}
