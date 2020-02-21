package com.aptasystems.kakapo.service;

import android.content.Context;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountBackupComplete;
import com.aptasystems.kakapo.event.UploadAccountComplete;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.request.UploadAccountRequest;
import kakapo.api.response.UploadAccountResponse;
import kakapo.crypto.KeyPair;
import kakapo.crypto.PGPEncryptionService;
import kakapo.crypto.SecretKeyEncryptionService;
import kakapo.crypto.exception.CryptoException;
import kakapo.crypto.exception.EncryptFailedException;
import kakapo.crypto.exception.KeyGenerationException;
import kakapo.crypto.exception.SignMessageException;
import kakapo.util.HashUtil;
import kakapo.util.TimeUtil;

@Singleton
public class AccountBackupService {

    private static final int BACKUP_VERSION_NUMBER = 1;

    private Context _context;
    private EntityDataStore<Persistable> _entityStore;
    private SecretKeyEncryptionService _secretKeyEncryptionService;
    private EventBus _eventBus;
    private TemporaryFileService _temporaryFileService;
    private PGPEncryptionService _pgpEncryptionService;
    private RetrofitWrapper _retrofitWrapper;

    @Inject
    AccountBackupService(Context context,
                         EntityDataStore<Persistable> entityStore,
                         SecretKeyEncryptionService secretKeyEncryptionService,
                         EventBus eventBus,
                         TemporaryFileService temporaryFileService,
                         PGPEncryptionService pgpEncryptionService,
                         RetrofitWrapper retrofitWrapper) {
        _context = context;
        _entityStore = entityStore;
        _secretKeyEncryptionService = secretKeyEncryptionService;
        _eventBus = eventBus;
        _temporaryFileService = temporaryFileService;
        _pgpEncryptionService = pgpEncryptionService;
        _retrofitWrapper = retrofitWrapper;
    }

    public Disposable uploadAccountShareAsync(long userAccountId, String hashedPassword) {
        return Observable.fromCallable(() -> uploadAccountShare(userAccountId, hashedPassword))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(UploadAccountComplete.success(result)), throwable -> {
                    throwable.printStackTrace();
                    ApiException apiException = (ApiException) throwable;
                    _eventBus.post(UploadAccountComplete.failure(apiException.getErrorCode()));
                });
    }

    private AccountBackupInfo uploadAccountShare(long userAccountId, String hashedPassword) throws ApiException {

        // Generate a password by getting some random data and hashing it with sha256.
        byte[] passwordBuffer = new byte[2048];
        new SecureRandom().nextBytes(passwordBuffer);
        String backupPassword = HashUtil.sha256ToString(passwordBuffer);

        // Serialize the user account.
        byte[] serializedUserAccount;
        try {
            serializedUserAccount = serializeUserAccountData(userAccountId);
        } catch (IOException e) {
            throw new ApiException(e, AsyncResult.AccountSerializationFailed);
        }

        // Generate a salt, much like we generated the password.
        byte[] saltBuffer = new byte[2048];
        new SecureRandom().nextBytes(saltBuffer);
        String salt = HashUtil.sha256ToString(saltBuffer);

        // Encrypt the backup.
        byte[] encryptedBackup;
        try {
            encryptedBackup = _secretKeyEncryptionService.encryptToByteArray(backupPassword,
                    salt,
                    serializedUserAccount);
        } catch (CryptoException e) {
            throw new ApiException(e, AsyncResult.EncryptionFailed);
        }

        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);

        // Build the request.
        UploadAccountRequest uploadAccountRequest = new UploadAccountRequest();
        uploadAccountRequest.setGuid(userAccount.getGuid());
        uploadAccountRequest.setEncryptedAccountData(encryptedBackup);

        // Sign the request.
        try {
            KeyPair keyPair = new KeyPair(userAccount.getSecretKeyRings(),
                    userAccount.getPublicKeyRings());
            byte[] signature = _pgpEncryptionService.sign(uploadAccountRequest.getMessageDigest(),
                    keyPair,
                    userAccount.getGuid(),
                    hashedPassword);
            uploadAccountRequest.setSignature(signature);
        } catch (SignMessageException e) {
            throw new ApiException(e, AsyncResult.IncorrectPassword);
        }

        // Make HTTP call.
        UploadAccountResponse response = _retrofitWrapper.uploadAccount(uploadAccountRequest);

        return new AccountBackupInfo(response.getGuid(), backupPassword, salt);
    }

    public Disposable createAccountBackupAsync(long userAccountId, String password) {
        return Observable.fromCallable(() -> createAccountBackup(userAccountId, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> _eventBus.post(AccountBackupComplete.success(result)), throwable -> {
                    throwable.printStackTrace();
                    if (throwable instanceof IOException) {
                        _eventBus.post(AccountBackupComplete.serializationError());
                    } else if (throwable instanceof CryptoException) {
                        _eventBus.post(AccountBackupComplete.encryptionError());
                    }
                });
    }

    private File createAccountBackup(long userAccountId, String password)
            throws IOException, KeyGenerationException, EncryptFailedException {

        // Serialize the user account.
        byte[] serializedUserAccount = serializeUserAccountData(userAccountId);

        // This is somewhat kludgy. We need a salt, but we don't really care about it as we're not
        // going to store it anywhere, so just hash the password to obtain the salt.
        String salt = HashUtil.sha256ToString(password);

        // Encrypt the backup.
        byte[] encryptedBackup =
                _secretKeyEncryptionService.encryptToByteArray(password,
                        salt,
                        serializedUserAccount);

        // Write the backup to a file.
        return writeToFile(encryptedBackup);
    }

    private byte[] serializeUserAccountData(long userAccountId) throws IOException {
        return new AccountSerializerV1(_entityStore).serializeUserAccountData(userAccountId);
    }

    private File writeToFile(byte[] data) throws IOException {

        String filename = _context.getString(R.string.account_backup_file_prefix) + TimeUtil.timecodeInZulu();

        // Write the encrypted data to a file so it can be shared. This file will get cleaned up
        // along with all the other temporary files the next time MainActivity runs.
        File backupFile = _temporaryFileService.newTempFile(filename);
        FileOutputStream fileOutputStream = new FileOutputStream(backupFile);
        fileOutputStream.write(data);
        fileOutputStream.close();
        return backupFile;
    }
}
