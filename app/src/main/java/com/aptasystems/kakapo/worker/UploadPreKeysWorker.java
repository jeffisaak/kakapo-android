package com.aptasystems.kakapo.worker;

import android.content.Context;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.service.NotificationService;
import com.aptasystems.kakapo.service.UserAccountService;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class UploadPreKeysWorker extends Worker {

    public static final String KEY_USER_ACCOUNT_ID = "userAccountId";
    public static final String KEY_PASSWORD = "password";

    @Inject
    UserAccountService _userAccountService;

    @Inject
    NotificationService _notificationService;

    public UploadPreKeysWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        ((KakapoApplication) context).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public Result doWork() {

        Long userAccountId = getInputData().getLong(KEY_USER_ACCOUNT_ID, 0L);
        String password = getInputData().getString(KEY_PASSWORD);

        try {
            _userAccountService.generateAndUploadPreKeys(userAccountId, password);
            _notificationService.hidePreKeyCreationErrorNotification();
        } catch (ApiException e) {

            _notificationService.showPreKeyCreationErrorNotification();

            // Reschedule our prekey generation job.
            Data uploadPreKeysData = new Data.Builder()
                    .putLong(UploadPreKeysWorker.KEY_USER_ACCOUNT_ID, userAccountId)
                    .putString(UploadPreKeysWorker.KEY_PASSWORD, password)
                    .build();
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(UploadPreKeysWorker.class)
                    .setInputData(uploadPreKeysData)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("uploadPreKeys",
                    ExistingWorkPolicy.REPLACE,
                    workRequest);

            return Result.failure();
        }

        return Result.success();
    }
}
