package com.aptasystems.kakapo.worker;

import android.content.Context;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.service.AccountBackupService;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import kakapo.api.response.BackupAccountResponse;

public class AccountBackupWorker extends Worker {

    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_PASSWORD = "password";

    @Inject
    AccountBackupService _accountBackupService;

    public AccountBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        ((KakapoApplication) context).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public Result doWork() {

        Long accountId = getInputData().getLong(KEY_ACCOUNT_ID, 0L);
        String password = getInputData().getString(KEY_PASSWORD);

        try {
            // Backup the account to the Kakapo server.
            _accountBackupService.uploadAccountBackup(accountId, password);
        } catch (ApiException e) {
            // TODO: Handle exception somehow.
//            return Result.failure()
//            e.printStackTrace();
        }

        return Result.success();
    }
}
