package com.aptasystems.kakapo.service;

import android.content.Context;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.UploadAccountComplete;
import com.aptasystems.kakapo.exception.AccountEncryptionFailedException;
import com.aptasystems.kakapo.exception.AccountSerializationFailedException;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.BadRequestException;
import com.aptasystems.kakapo.exception.BackupVersionNumberConflictException;
import com.aptasystems.kakapo.exception.OtherHttpErrorException;
import com.aptasystems.kakapo.exception.PayloadTooLargeException;
import com.aptasystems.kakapo.exception.RetrofitIOException;
import com.aptasystems.kakapo.exception.ServerUnavailableException;
import com.aptasystems.kakapo.exception.TooManyRequestsException;
import com.aptasystems.kakapo.exception.UnauthorizedException;
import com.goterl.lazycode.lazysodium.LazySodium;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.api.response.BackupAccountResponse;
import kakapo.crypto.HashAndEncryptResult;
import kakapo.crypto.ICryptoService;
import kakapo.crypto.exception.EncryptFailedException;

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
                .subscribe(
                        result -> {
                            // Post an event.
                            UserAccount userAccount = _userAccountDAO.find(userAccountId);
                            AccountBackupInfo backupInfo = new AccountBackupInfo(userAccount.getGuid(),
                                    userAccount.getApiKey(),
                                    userAccount.getPasswordSalt());
                            _eventBus.post(UploadAccountComplete.success(backupInfo));
                        },
                        throwable -> {
                            ApiException apiException = (ApiException) throwable;
                            _eventBus.post(UploadAccountComplete.failure(apiException.getErrorCode()));
                        });
    }

    public BackupAccountResponse uploadAccountBackup(Long userAccountId, String password)
            throws AccountSerializationFailedException,
            AccountEncryptionFailedException,
            RetrofitIOException,
            PayloadTooLargeException,
            BadRequestException,
            ServerUnavailableException,
            BackupVersionNumberConflictException,
            OtherHttpErrorException,
            TooManyRequestsException,
            UnauthorizedException {

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
            throw new AccountSerializationFailedException(e);
        }

        // Encrypt the data.
        HashAndEncryptResult hashAndEncryptResult;
        try {
            hashAndEncryptResult = _cryptoService.encryptAccountData(serializedUserAccount,
                    userAccount.getPasswordSalt(),
                    password);
        } catch (EncryptFailedException e) {
            throw new AccountEncryptionFailedException(e);
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