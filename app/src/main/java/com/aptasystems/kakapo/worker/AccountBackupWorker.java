package com.aptasystems.kakapo.worker;

import android.content.Context;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.service.AccountBackupService;
import com.aptasystems.kakapo.service.NotificationService;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class AccountBackupWorker extends Worker {

    public static final String KEY_USER_ACCOUNT_ID = "userAccountId";
    public static final String KEY_PASSWORD = "password";

    @Inject
    AccountBackupService _accountBackupService;

    @Inject
    NotificationService _notificationService;

    public AccountBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        ((KakapoApplication) context).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public Result doWork() {

        long userAccountId = getInputData().getLong(KEY_USER_ACCOUNT_ID, 0L);
        String password = getInputData().getString(KEY_PASSWORD);

        try {
            _accountBackupService.uploadAccountBackup(userAccountId, password);
            _notificationService.hideBackupErrorNotification();
        } catch (ApiException e) {

            _notificationService.showBackupErrorNotification();

            // Reschedule our backup job.
            Data accountBackupData = new Data.Builder()
                    .putLong(AccountBackupWorker.KEY_USER_ACCOUNT_ID, userAccountId)
                    .putString(AccountBackupWorker.KEY_PASSWORD, password)
                    .build();
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AccountBackupWorker.class)
                    .setInputData(accountBackupData)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("backupAccountData-" + userAccountId,
                    ExistingWorkPolicy.REPLACE,
                    workRequest);

            return Result.failure();
        }

        return Result.success();
    }
}
