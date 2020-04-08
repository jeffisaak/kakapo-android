package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.PreKeyDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountCreationComplete;
import com.aptasystems.kakapo.event.AccountDeletionComplete;
import com.aptasystems.kakapo.event.AuthenticationComplete;
import com.aptasystems.kakapo.event.BlacklistAuthorComplete;
import com.aptasystems.kakapo.event.QuotaComplete;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.util.ColourUtil;
import com.goterl.lazycode.lazysodium.LazySodium;
import com.goterl.lazycode.lazysodium.utils.Key;
import com.goterl.lazycode.lazysodium.utils.KeyPair;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kakapo.api.request.SignUpRequest;
import kakapo.api.request.UploadPreKeysRequest;
import kakapo.api.response.QuotaResponse;
import kakapo.api.response.SignUpResponse;
import kakapo.api.response.UploadPreKeysResponse;
import kakapo.crypto.HashAndEncryptResult;
import kakapo.crypto.ICryptoService;
import kakapo.crypto.exception.DecryptFailedException;
import kakapo.crypto.exception.EncryptFailedException;
import kakapo.crypto.exception.KeyGenerationException;
import kakapo.crypto.exception.SignMessageException;
import kakapo.crypto.exception.SignatureVerificationFailedException;

@Singleton
public class UserAccountService {

    private static final String TAG = UserAccountService.class.getSimpleName();

    @Inject
    ICryptoService _cryptoService;

    @Inject
    RetrofitWrapper _retrofitWrapper;

    @Inject
    ColourUtil _colourUtil;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    PreKeyDAO _preKeyDAO;

    @Inject
    EventBus _eventBus;

    @Inject
    public UserAccountService(KakapoApplication application) {
        // Dependency injection.
        application.getKakapoComponent().inject(this);
    }

    public boolean checkPassword(String password,
                                 String salt,
                                 String nonce,
                                 String encryptedSigningSecretKey) {

        // Attempt to decrypt the encrypted secret key.
        try {
            _cryptoService.decryptSigningKey(password,
                    salt,
                    nonce,
                    LazySodium.toBin(encryptedSigningSecretKey));
            return true;
        } catch (DecryptFailedException e) {
            // Yeah, I know this is bad practice. I'm doing it anyway.
            return false;
        }
    }

    public Disposable createNewAccountAsync(String name, String password) {
        return Observable.fromCallable(() -> createNewAccount(name, password))
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

    private String createNewAccount(String name, String password) throws ApiException {

        // Generate signing keypair.
        KeyPair signingKeyPair;
        try {
            signingKeyPair = _cryptoService.generateSigningKeyPair();
        } catch (KeyGenerationException e) {
            throw new ApiException(e, AsyncResult.KeyGenerationFailed);
        }

        // Build the request.
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setSigningPublicKey(signingKeyPair.getPublicKey().getAsHexString());

        // Make HTTP call.
        SignUpResponse signUpResponse = _retrofitWrapper.createAccount(signUpRequest);

        // Encrypt our signing secret key.
        HashAndEncryptResult secretKeyHashEncryptResult;
        try {
            secretKeyHashEncryptResult =
                    _cryptoService.encryptSigningKey(signingKeyPair.getSecretKey(), password);
        } catch (EncryptFailedException e) {
            throw new ApiException(e, AsyncResult.KeyGenerationFailed);
        }

        // Convert the encrypted signing secret key, salt, and nonce to hex strings for storage.
        String passwordSalt =
                LazySodium.toHex(secretKeyHashEncryptResult.getHashResult().getSalt());
        String encryptedSigningSecretKey =
                LazySodium.toHex(secretKeyHashEncryptResult.getEncryptionResult().getCiphertext());
        String signingSecretKeyNonce =
                LazySodium.toHex(secretKeyHashEncryptResult.getEncryptionResult().getNonce());

        // Persist the user account on the device.
        UserAccount userAccount = new UserAccount();
        userAccount.setGuid(signUpResponse.getGuid());
        userAccount.setName(name);
        userAccount.setPasswordSalt(passwordSalt);
        userAccount.setSigningPublicKey(signingKeyPair.getPublicKey().getAsHexString());
        userAccount.setEncryptedSigningSecretKey(encryptedSigningSecretKey);
        userAccount.setSigningSecretKeyNonce(signingSecretKeyNonce);
        userAccount.setApiKey(signUpResponse.getApiKey());
        userAccount.setColour(_colourUtil.randomColour());
        userAccount.setBackupRequired(false);
        _userAccountDAO.insert(userAccount);

        // Generate and upload some prekeys.
        generateAndUploadPreKeys(userAccount.getId(), password);

        // Return the GUID.
        return signUpResponse.getGuid();
    }

    public void generateAndUploadPreKeys(Long userAccountId, String password) throws ApiException {

        UserAccount userAccount = _userAccountDAO.find(userAccountId);

        // Extract and decrypt the signing secret key.
        byte[] encryptedSigningSecretKey = LazySodium.toBin(userAccount.getEncryptedSigningSecretKey());
        Key signingPublicKey = Key.fromHexString(userAccount.getSigningPublicKey());
        Key signingSecretKey;
        try {
            byte[] secretSigningKeyBytes = _cryptoService.decryptSigningKey(password,
                    userAccount.getPasswordSalt(),
                    userAccount.getSigningSecretKeyNonce(),
                    encryptedSigningSecretKey);
            signingSecretKey = Key.fromBytes(secretSigningKeyBytes);
        } catch (DecryptFailedException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Generate prekeys.
        Map<String, KeyPair> keyExchangeKeyPairs = new HashMap<>();
        // TODO: Extract number of prekeys generated into configuration.
        for (int ii = 0; ii < 100; ii++) {
            KeyPair keyExchangeKeyPair = _cryptoService.generateKeyExchangeKeypair();
            keyExchangeKeyPairs.put(keyExchangeKeyPair.getPublicKey().getAsHexString(), keyExchangeKeyPair);
        }

        // Sign prekeys.
        List<String> signedPreKeys = new ArrayList<>();
        try {
            for (String publicPreKey : keyExchangeKeyPairs.keySet()) {
                KeyPair keyExchangeKeyPair = keyExchangeKeyPairs.get(publicPreKey);
                byte[] signedPreKey = _cryptoService.signPreKey(keyExchangeKeyPair.getPublicKey(), signingSecretKey);
                signedPreKeys.add(LazySodium.toHex(signedPreKey));
            }
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.KeyGenerationFailed);
        }

        // Upload the signed prekeys.
        UploadPreKeysRequest uploadPreKeysRequest = new UploadPreKeysRequest();
        uploadPreKeysRequest.setSignedPreKeys(signedPreKeys);
        UploadPreKeysResponse uploadPreKeysResponse =
                _retrofitWrapper.uploadPreKeys(userAccount.getGuid(),
                        userAccount.getApiKey(),
                        uploadPreKeysRequest);

        // The response contains the uploaded signed prekeys along with their ID. We need to save
        // the prekeys with the IDs.
        for (Long preKeyId : uploadPreKeysResponse.getIdToKeyMap().keySet()) {
            Key fetchedPreKey = Key.fromHexString(uploadPreKeysResponse.getIdToKeyMap().get(preKeyId));

            // Verify the signature (this is a bit ridiculous that we're verifying the things we
            // just signed, but we want to store them unsigned).
            byte[] preKeyBytes;
            try {
                preKeyBytes = _cryptoService.verifyPreKey(fetchedPreKey.getAsBytes(), signingPublicKey);
            } catch (SignatureVerificationFailedException e) {
                throw new ApiException(e, AsyncResult.KeyVerificationFailed);
            }

            // Store the prekey.
            String preKeyPublicKey = LazySodium.toHex(preKeyBytes);
            _preKeyDAO.insert(userAccount,
                    preKeyId,
                    preKeyBytes,
                    keyExchangeKeyPairs.get(preKeyPublicKey).getSecretKey());
        }
    }

    public Disposable deleteAccountFromServerAsync(UserAccount userAccount, String password) {

        return Completable.fromCallable(() -> deleteAccountFromServer(userAccount, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> _eventBus.post(AccountDeletionComplete.success(true)), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AccountDeletionComplete.failure(apiException.getErrorCode()));
                });
    }

    private Void deleteAccountFromServer(UserAccount userAccount, String password)
            throws ApiException {

        // Make HTTP call.
        _retrofitWrapper.deleteAccount(userAccount.getId(), password);

        // Delete the account from the device.
        deleteAccountFromDevice(userAccount);

        return null;
    }

    public void deleteAccountFromDevice(UserAccount userAccount) {
        // Delete the user account - everything else will cascade.
        _userAccountDAO.delete(userAccount);
    }

    public Disposable blacklistAuthorAsync(long userAccountId, String password, String guidToBlacklist) {
        return Completable.fromCallable(() -> blacklistAuthor(userAccountId, password, guidToBlacklist))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> _eventBus.post(BlacklistAuthorComplete.success(guidToBlacklist)),
                        throwable -> {
                            ApiException apiException = (ApiException) throwable;
                            _eventBus.post(BlacklistAuthorComplete.failure(apiException.getErrorCode()));
                        });
    }

    private Void blacklistAuthor(long userAccountId, String password, String guidToBlacklist)
            throws ApiException {
        _retrofitWrapper.blacklist(guidToBlacklist, userAccountId, password);
        return null;
    }

    public Disposable authenticateAsync(UserAccount userAccount, String password) {
        return Completable.fromCallable(() -> authenticate(userAccount, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    // Post result.
                    _eventBus.post(AuthenticationComplete.success(userAccount.getId(), password));
                }, throwable -> {
                    throwable.printStackTrace();
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(AuthenticationComplete.failure(apiException.getErrorCode()));
                });
    }

    private Void authenticate(UserAccount userAccount, String password) throws ApiException {
        _retrofitWrapper.authenticate(userAccount.getId(), password);
        return null;
    }

    public Disposable getQuotaAsync(UserAccount userAccount, String password) {
        return Observable.fromCallable(() -> getQuota(userAccount, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(QuotaComplete.success(result.getUsedQuota(), result.getMaxQuota())), throwable -> {
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(QuotaComplete.failure(apiException.getErrorCode()));
                });
    }

    private QuotaResponse getQuota(UserAccount userAccount, String password) throws ApiException {
        return _retrofitWrapper.fetchQuota(userAccount.getId(), password);
    }
}
