package com.aptasystems.kakapo.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

@Singleton
public class NotificationService {

    private static final String NOTIFICATION_CHANNEL_ID = "kakapo-errors";

    private static final int NOTIFICATION_ID_BACKUPS = 100;
    private static final int NOTIFICATION_ID_PREKEYS = 101;

    @Inject
    Context _context;

    @Inject
    NotificationService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public void showBackupErrorNotification() {

        createErrorsNotificationChannel();

        String message = _context.getString(R.string.notification_backup_error_big_text);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(_context,
                NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(_context.getString(R.string.notification_backup_error_title))
                .setContentText(_context.getString(R.string.notification_backup_error_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.notify(NOTIFICATION_ID_BACKUPS, builder.build());
    }

    public void hideBackupErrorNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.cancel(NOTIFICATION_ID_BACKUPS);
    }

    public void showPreKeyCreationErrorNotification() {
        createErrorsNotificationChannel();

        String message = _context.getString(R.string.notification_pre_key_error_big_text);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(_context,
                NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(_context.getString(R.string.notification_pre_key_error_title))
                .setContentText(_context.getString(R.string.notification_pre_key_error_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.notify(NOTIFICATION_ID_PREKEYS, builder.build());
    }

    public void hidePreKeyCreationErrorNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
        notificationManager.cancel(NOTIFICATION_ID_PREKEYS);
    }

    private void createErrorsNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(_context);
            CharSequence channelName = _context.getString(R.string.notification_channel_errors_name);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
