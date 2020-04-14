package com.aptasystems.kakapo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.databinding.ActivityMainBinding;
import com.aptasystems.kakapo.dialog.ShareAccountDialog;
import com.aptasystems.kakapo.dialog.ShareIdDialog;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.RestoreRemoteBackupComplete;
import com.aptasystems.kakapo.event.SubmitItemComplete;
import com.aptasystems.kakapo.event.UploadAccountComplete;
import com.aptasystems.kakapo.event.UserAccountRenamed;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.fragment.FriendListFragment;
import com.aptasystems.kakapo.fragment.GroupListFragment;
import com.aptasystems.kakapo.fragment.MeFragment;
import com.aptasystems.kakapo.fragment.NewsFragment;
import com.aptasystems.kakapo.service.AccountBackupInfo;
import com.aptasystems.kakapo.service.AccountBackupService;
import com.aptasystems.kakapo.service.AccountRestoreService;
import com.aptasystems.kakapo.service.AnnouncementService;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.service.TemporaryFileService;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.util.ShareUtil;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject
    AnnouncementService _announcementService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    ShareService _shareItemService;

    @Inject
    EventBus _eventBus;

    @Inject
    AccountBackupService _accountBackupService;

    @Inject
    AccountRestoreService _accountRestoreService;

    @Inject
    TemporaryFileService _temporaryFileService;

    private CompositeDisposable _compositeDisposable = new CompositeDisposable();
    private ActivityMainBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Perform dependency injection.
        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        // Reset app data if necessary.
        int lastVersionNumber = _prefsUtil.getVersionNumber(VersionCodes.NOT_TRACKED);
        if (lastVersionNumber == VersionCodes.RESET_ANNOUNCEMENT &&
                BuildConfig.VERSION_CODE == VersionCodes.LIBSODIUM_REWRITE) {

            // Clear the credentials.
            _prefsUtil.clearCredentials();

            // Delete database.
            String databaseName = getString(R.string.local_database_name);
            deleteDatabase(databaseName);
        }

        // Before we go any further, check to see if the intro has been shown, and if not, show it.
        if (!_prefsUtil.isIntroShown()) {
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Set up the layout and action bar.
        _binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(_binding.getRoot());
        setSupportActionBar(_binding.toolbar);

        // If we aren't signed in, redirect to the select user account activity.
        if (_prefsUtil.getCurrentUserAccountId() == null ||
                _prefsUtil.getCurrentPassword() == null) {
            Intent intent = new Intent(this, SelectUserAccountActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        _binding.includes.viewPager.setOffscreenPageLimit(3);

        // Create the adapter that will return fragments.
        SectionsPagerAdapter pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        // Set up the ViewPager with the sections adapter.
        _binding.includes.viewPager.setAdapter(pagerAdapter);

        _binding.includes.viewPager.setCurrentItem(_prefsUtil.getCurrentTabIndex(SectionsPagerAdapter.FRAGMENT_INDEX_FRIENDS));

        // Set up the tab layout.
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(_binding.includes.viewPager);

        // Perform cleanup of any temporary files.
        _temporaryFileService.cleanupAsync();

        // Get the backup version from the server and perform any needed account merge.
        _accountRestoreService.checkAndMergeRemoteBackupAsync(_prefsUtil.getCurrentUserAccountId(),
                _prefsUtil.getCurrentPassword());

        // Show any necessary announcments.
        _announcementService.showAnnouncements(getSupportFragmentManager());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_prefsUtil.getCurrentUserAccountId() != null) {
            UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
            setTitle(userAccount.getName());
        }

        // Register to listen for events.
        if (!_eventBus.isRegistered(this)) {
            _eventBus.register(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Save the current view pager item for when we come back here.
        _prefsUtil.setCurrentTabIndex(_binding.includes.viewPager.getCurrentItem());

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _compositeDisposable.dispose();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void signOut(MenuItem menuItem) {
        _prefsUtil.clearCredentials();
        Intent intent = new Intent(this, SelectUserAccountActivity.class);
        startActivity(intent);
        finish();
    }

    public void showHelp(MenuItem menuItem) {

        // Show help based on which fragment is visible.
        int helpResourceId = 0;
        switch (_binding.includes.viewPager.getCurrentItem()) {
            case SectionsPagerAdapter.FRAGMENT_INDEX_ME:
                helpResourceId = R.raw.help_activity_main_fragment_me;
                break;
            case SectionsPagerAdapter.FRAGMENT_INDEX_NEWS_FEED:
                helpResourceId = R.raw.help_activity_main_fragment_news;
                break;
            case SectionsPagerAdapter.FRAGMENT_INDEX_FRIENDS:
                helpResourceId = R.raw.help_activity_main_fragment_friends;
                break;
            case SectionsPagerAdapter.FRAGMENT_INDEX_GROUPS:
                helpResourceId = R.raw.help_activity_main_fragment_groups;
                break;
        }

        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, helpResourceId);
        startActivity(intent);
    }

    public void shareIdAsQrCode(MenuItem menuItem) {

        // Show the share ID dialog.
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
        ShareIdDialog dialog = ShareIdDialog.newInstance(userAccount.getGuid());
        dialog.show(getSupportFragmentManager(), "shareIdDialog");
    }

    public void shareIdAsText(MenuItem menuItem) {

        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
        Intent shareIntent = ShareUtil.buildShareIntent(userAccount.getGuid());
        Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.app_title_share_id_with));
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            Snackbar.make(_binding.coordinatorLayout,
                    R.string.app_snack_error_no_id_share_targets,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    public void shareAccountAsQrCode(MenuItem menuItem) {

        // Start the account upload if necessary.
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
        if (userAccount.isBackupRequired()) {

            Snackbar.make(_binding.coordinatorLayout,
                    R.string.main_snack_sharing_account,
                    Snackbar.LENGTH_SHORT).show();

            Disposable disposable =
                    _accountBackupService.uploadAccountBackupAsync(_prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentPassword());
            _compositeDisposable.add(disposable);

        } else {
            // Show the share account dialog.
            ShareAccountDialog dialog =
                    ShareAccountDialog.newInstance(new AccountBackupInfo(userAccount.getGuid(), userAccount.getApiKey(), userAccount.getPasswordSalt()));
            dialog.show(getSupportFragmentManager(), "shareAccountDialog");
        }
    }

    public void shareAccountAsText(MenuItem menuItem) {

        // Start the account upload if necessary.
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
        if (userAccount.isBackupRequired()) {

            Snackbar.make(_binding.coordinatorLayout,
                    R.string.main_snack_sharing_account,
                    Snackbar.LENGTH_SHORT).show();

            Disposable disposable =
                    _accountBackupService.uploadAccountBackupAsync(_prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentPassword());
            _compositeDisposable.add(disposable);

        } else {

            AccountBackupInfo accountBackupInfo = new AccountBackupInfo(userAccount.getGuid(),
                    userAccount.getApiKey(),
                    userAccount.getPasswordSalt());

            Intent shareIntent = ShareUtil.buildShareIntent(accountBackupInfo.toString());
            Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.app_title_share_id_with));
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(chooserIntent);
            } else {
                Snackbar.make(_binding.coordinatorLayout,
                        R.string.app_snack_error_no_id_share_targets,
                        Snackbar.LENGTH_SHORT).show();
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(RestoreRemoteBackupComplete event) {
        if (event.getStatus() == AsyncResult.Success) {
            // Show the user a snack
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    R.string.account_restore_complete_snack,
                    Snackbar.LENGTH_LONG);
            snackbar.show();
        } else {

            // FUTURE: Implement finer-grained error messages.
            @StringRes
            int errorMessageId = 0;
            int snackbarLength = Snackbar.LENGTH_LONG;
            switch (event.getStatus()) {
                case RetrofitIOException:
                case BadRequest:
                case ServerUnavailable:
                case TooManyRequests:
                case OtherHttpError:
                case Unauthorized:
                case NotFound:
                case ContentStreamFailed:
                case AccountDeserializationFailed:
                case DecryptionFailed:
                    errorMessageId = R.string.app_snack_error_account_sync;
                    break;
            }
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            snackbar.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadAccountComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Show the share account dialog.
            ShareAccountDialog dialog =
                    ShareAccountDialog.newInstance(event.getAccountBackupInfo());
            dialog.show(getSupportFragmentManager(), "shareAccountDialog");

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            boolean forceSignOut = false;
            switch (event.getStatus()) {
                case AccountSerializationFailed:
                    errorMessageId = R.string.snack_error_account_serialization_failed;
                    // FUTURE: Help link would be nice.
                    break;
                case AccountEncryptionFailed:
                    errorMessageId = R.string.fragment_me_snack_error_account_upload_encrypt_failed;
                    // FUTURE: Help link would be nice.
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
                case PayloadTooLarge:
                    // This really shouldn't happen in normal operation.
                    errorMessageId = R.string.fragment_me_snack_error_account_upload_payload_too_large;
                    // FUTURE: Help link would be nice.
                    break;
                case BadRequest:
                    errorMessageId = R.string.app_snack_error_bad_request;
                    // FUTURE: Help link would be nice.
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case Conflict:
                    errorMessageId = R.string.app_snack_error_account_version_conflict;
                    helpResId = R.raw.help_error_account_version_conflict;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    // FUTURE: Help link would be nice.
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case Unauthorized:
                    errorMessageId = R.string.app_snack_error_unauthorized;
                    forceSignOut = true;
                    break;
            }

            // Give the user a snack. Yum.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            if (forceSignOut) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        _prefsUtil.clearCredentials();
                        Intent intent = new Intent(MainActivity.this,
                                SelectUserAccountActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
            snackbar.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SubmitItemComplete event) {

        if (event.getEventTarget() != MainActivity.class) {
            return;
        }

        // Only interested in showing toasts here.
        if (event.getStatus() == AsyncResult.Success) {
            Toast.makeText(this,
                    R.string.main_toast_item_submission_success,
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    R.string.main_toast_item_submission_failed,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UserAccountRenamed event) {
        setTitle(event.getName());
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the
     * sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private static final int FRAGMENT_INDEX_ME = 0;
        private static final int FRAGMENT_INDEX_NEWS_FEED = 1;
        private static final int FRAGMENT_INDEX_FRIENDS = 2;
        private static final int FRAGMENT_INDEX_GROUPS = 3;
        private static final int PAGE_COUNT = 4;

        public SectionsPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case FRAGMENT_INDEX_ME:
                    return MeFragment.newInstance();
                case FRAGMENT_INDEX_NEWS_FEED:
                    return NewsFragment.newInstance();
                case FRAGMENT_INDEX_FRIENDS:
                    return FriendListFragment.newInstance();
                case FRAGMENT_INDEX_GROUPS:
                    return GroupListFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FRAGMENT_INDEX_ME:
                    return getResources().getString(R.string.fragment_me_title);
                case FRAGMENT_INDEX_NEWS_FEED:
                    return getResources().getString(R.string.fragment_news_title);
                case FRAGMENT_INDEX_FRIENDS:
                    return getResources().getString(R.string.fragment_friends_title);
                case FRAGMENT_INDEX_GROUPS:
                    return getResources().getString(R.string.fragment_groups_title);
            }
            return null;
        }
    }
}
