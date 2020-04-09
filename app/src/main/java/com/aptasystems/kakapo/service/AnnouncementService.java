package com.aptasystems.kakapo.service;

import android.content.Context;

import com.aptasystems.kakapo.BuildConfig;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.VersionCodes;
import com.aptasystems.kakapo.dialog.AnnouncementDialog;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountBackupComplete;
import com.aptasystems.kakapo.event.UploadAccountComplete;
import com.aptasystems.kakapo.exception.ApiException;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.fragment.app.FragmentManager;
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
public class AnnouncementService {

    private Context _context;
    private PrefsUtil _prefsUtil;

    @Inject
    AnnouncementService(Context context,
                        PrefsUtil prefsUtil) {
        _context = context;
        _prefsUtil = prefsUtil;
    }

    public void showAnnouncements(FragmentManager fragmentManager) {

        // Get the last version number from the preferences.
        int lastVersionNumber = _prefsUtil.getVersionNumber(VersionCodes.NOT_TRACKED);

        // Reset announcement.
        if (lastVersionNumber == VersionCodes.NOT_TRACKED &&
                BuildConfig.VERSION_CODE == VersionCodes.RESET_ANNOUNCEMENT) {
            AnnouncementDialog dialog = AnnouncementDialog.newInstance(R.string.announcement_app_reset_title,
                    R.string.announcement_app_reset_content);
            dialog.show(fragmentManager, "announcementDialog");
        }

        // Add further announcements here. Be sure to structure the condition around it appropriately.

        // Store the current version number in the prefs.
        _prefsUtil.setVersionNumber(BuildConfig.VERSION_CODE);
    }
}
