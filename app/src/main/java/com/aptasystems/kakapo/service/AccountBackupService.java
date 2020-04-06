package com.aptasystems.kakapo.service;

import android.content.Context;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountBackupComplete;
import com.aptasystems.kakapo.event.UploadAccountComplete;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.goterl.lazycode.lazysodium.LazySodium;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.response.BackupAccountResponse;
import kakapo.crypto.HashAndEncryptResult;
import kakapo.crypto.ICryptoService;
import kakapo.crypto.exception.CryptoException;
import kakapo.crypto.exception.EncryptFailedException;
import kakapo.crypto.exception.KeyGenerationException;
import kakapo.util.SerializationUtil;
import kakapo.util.TimeUtil;

@Singleton
public class AccountBackupService {

    @Inject
    Context _context;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    EventBus _eventBus;

    @Inject
    TemporaryFileService _temporaryFileService;

    @Inject
    ICryptoService _cryptoService;

    @Inject
    RetrofitWrapper _retrofitWrapper;

    @Inject
    public AccountBackupService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public Disposable uploadAccountBackupAsync(long userAccountId, String password) {
        return Maybe.fromCallable(() -> uploadAccountBackup(userAccountId, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                            // Post an event.
                            UserAccount userAccount = _userAccountDAO.find(userAccountId);
                            AccountBackupInfo backupInfo = new AccountBackupInfo(userAccount.getGuid(),
                                    userAccount.getApiKey(),
                                    userAccount.getPasswordSalt());
                            _eventBus.post(UploadAccountComplete.success(backupInfo));
                        },
                        throwable -> {
                            // TODO: This could happen if we're out of sync with the server regarding the backup version. In this case, lets allow the user to sync with the server?
                            throwable.printStackTrace();
                            ApiException apiException = (ApiException) throwable;
                            _eventBus.post(UploadAccountComplete.failure(apiException.getErrorCode()));
                        });
    }

    public BackupAccountResponse uploadAccountBackup(Long userAccountId, String password) throws ApiException {

        if (userAccountId == null || password == null) {
            return null;
        }

        UserAccount userAccount = _userAccountDAO.find(userAccountId);

        // Serialize the user account.
        byte[] serializedUserAccount;
        try {
            serializedUserAccount = new AccountSerializerV1(_entityStore)
                    .serializeUserAccountData(userAccountId);
        } catch (IOException e) {
            throw new ApiException(e, AsyncResult.AccountSerializationFailed);
        }

        // Encrypt the data.
        HashAndEncryptResult hashAndEncryptResult;
        try {
            hashAndEncryptResult = _cryptoService.encryptAccountData(serializedUserAccount,
                    userAccount.getPasswordSalt(),
                    password);
        } catch (EncryptFailedException e) {
            throw new ApiException(e, AsyncResult.EncryptionFailed);
        }

        // Upload the backup.
        BackupAccountResponse response =
                _retrofitWrapper.uploadAccountBackup(userAccountId,
                        password,
                        userAccount.getRemoteBackupVersionNumber(),
                        LazySodium.toHex(hashAndEncryptResult.getEncryptionResult().getNonce()),
                        hashAndEncryptResult.getEncryptionResult().getCiphertext());

        // Update the user account record.
        _userAccountDAO.updateRemoteBackupVersionNumber(userAccountId,
                response.getBackupVersion());

        return response;
    }
}