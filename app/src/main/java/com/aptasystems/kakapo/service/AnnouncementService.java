package com.aptasystems.kakapo.service;

import android.content.Context;

import com.aptasystems.kakapo.BuildConfig;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.VersionCodes;
import com.aptasystems.kakapo.dialog.AnnouncementDialog;
import com.aptasystems.kakapo.util.PrefsUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.fragment.app.FragmentManager;

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

        // Reset complete.
        if (lastVersionNumber == VersionCodes.RESET_ANNOUNCEMENT &&
                BuildConfig.VERSION_CODE == VersionCodes.LIBSODIUM_REWRITE) {
            AnnouncementDialog dialog = AnnouncementDialog.newInstance(R.string.announcement_app_reset_complete_title,
                    R.string.announcement_app_reset_complete_content);
            dialog.show(fragmentManager, "announcementDialog");
        }

        // Add further announcements here. Be sure to structure the condition around it appropriately.

        // Store the current version number in the prefs.
        _prefsUtil.setVersionNumber(BuildConfig.VERSION_CODE);
    }
}
