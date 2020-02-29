package com.aptasystems.kakapo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.aptasystems.kakapo.dialog.EnterRestorePasswordDialog;
import com.aptasystems.kakapo.service.AccountRestoreService;
import com.aptasystems.kakapo.event.AccountDecryptCancelled;
import com.aptasystems.kakapo.event.AccountDecryptComplete;
import com.aptasystems.kakapo.event.AccountDecryptInProgress;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.viewmodel.RestoreAccountModel;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.ButterKnife;
import kakapo.util.TimeUtil;

public class RestoreAccountActivity extends AppCompatActivity {

    private static final String TAG = RestoreAccountActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 100;

    @Inject
    EventBus _eventBus;

    @Inject
    AccountRestoreService _accountRestoreService;

    @Inject
    PrefsUtil _prefsUtil;

    @BindView(R.id.layout_coordinator)
    CoordinatorLayout _coordinatorLayout;

    @BindView(R.id.layout_account_detail)
    ScrollView _accountDetailView;

    @BindView(R.id.text_view_backup_date)
    TextView _backupDateTextView;

    @BindView(R.id.text_view_user_account_id)
    TextView _userAccountIdTextView;

    @BindView(R.id.text_view_user_account_name)
    TextView _userAccountNameTextView;

    @BindView(R.id.text_view_group_count)
    TextView _groupCountTextView;

    @BindView(R.id.text_view_friend_count)
    TextView _friendCountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        setContentView(R.layout.activity_restore_account);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        // Ensure we have read external storage permission.
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            onCreateFinish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register to listen for events.
        if (!_eventBus.isRegistered(this)) {
            _eventBus.register(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_restore_account, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final RestoreAccountModel viewModel = new ViewModelProvider(this)
                .get(RestoreAccountModel.class);
        menu.findItem(R.id.action_restore_accounts).setVisible(
                viewModel.getAccountDataLiveData().getValue() != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onCreateFinish();
                } else {
                    // Tell the user we can't restore without permission.
                    Snackbar.make(_coordinatorLayout,
                            getString(R.string.restore_account_snack_permissions_unavailable),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.app_text_try_again), v -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(
                                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                            PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                                }
                            }).show();
                }
                return;
            }
        }
    }

    private void onCreateFinish() {

        final RestoreAccountModel viewModel = new ViewModelProvider(this)
                .get(RestoreAccountModel.class);

        // Observers.
        viewModel.getAccountDataLiveData().observe(this, accountData -> {

            if (accountData != null) {
                invalidateOptionsMenu();

                // Show the account detail view and populate it.
                String backupDate = DateUtils.formatDateTime(this,
                        accountData.getTimestampInGmt(),
                        DateUtils.FORMAT_SHOW_DATE |
                                DateUtils.FORMAT_SHOW_TIME |
                                DateUtils.FORMAT_SHOW_YEAR);
                _backupDateTextView.setText(backupDate);
                _userAccountIdTextView.setText(accountData.getUserAccount().getGuid());
                _userAccountNameTextView.setText(accountData.getUserAccount().getName());
                _groupCountTextView.setText(String.format(getString(R.string.restore_account_text_group_count), accountData.getGroups().size()));
                _friendCountTextView.setText(String.format(getString(R.string.restore_account_text_friend_count), accountData.getFriends().size()));

                _accountDetailView.setVisibility(View.VISIBLE);

            }
        });

        // If the account data has not been decrypted, go decrypt it.
        if (viewModel.getAccountDataLiveData().getValue() == null) {

            byte[] encryptedDataFromIntent = extractEncryptedDataFromIntent();

            // If we couldn't get any encrypted data from the intent, let the user know. If we could,
            // then pop up a dialog and ask for the password.
            if (encryptedDataFromIntent == null) {
                Toast.makeText(this,
                        R.string.restore_account_toast_error_reading_encrypted_data_from_intent,
                        Toast.LENGTH_LONG).show();
                finishAndRemoveTask();
            } else {
                // Show a dialog to collect the password for this backup.
                EnterRestorePasswordDialog dialog =
                        EnterRestorePasswordDialog.newInstance(encryptedDataFromIntent);
                dialog.show(getSupportFragmentManager(), "enterRestorePasswordDialog");
            }
        }
    }

    private byte[] extractEncryptedDataFromIntent() {
        // Extract the URI from the intent.
        Uri uri = extractUriFromIntent();
        byte[] encryptedData = null;
        if (uri != null) {
            try {
                encryptedData = IOUtils.toByteArray(getContentResolver().openInputStream(uri));
            } catch (IOException e) {
                // Swallow. We'll return null.
            }
        }
        return encryptedData;
    }

    private Uri extractUriFromIntent() {
        // Need to get the backup file from the intent.
        String action = getIntent().getAction();
        List<Uri> uris = new ArrayList<>();
        switch (action) {
            case Intent.ACTION_VIEW:
            case Intent.ACTION_EDIT:
                uris.add(getIntent().getData());
                break;
            case Intent.ACTION_SEND:
                // Only streams.
                uris.add(getIntent().<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
                break;
            case Intent.ACTION_SEND_MULTIPLE:
                // Only streams.
                uris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                break;
        }

        // There should be only one...
        if (uris.size() != 1) {
            return null;
        }
        return uris.get(0);
    }

    public void restoreAccount(MenuItem menuItem) {

        final RestoreAccountModel viewModel = new ViewModelProvider(this)
                .get(RestoreAccountModel.class);

        // Restore the account to the device.
        _accountRestoreService.restore(viewModel.getAccountDataLiveData().getValue());

        // Let the user know it happened with a Toast (as we'll be finishing this activity).
        Toast.makeText(this,
                R.string.restore_account_toast_restore_complete,
                Toast.LENGTH_LONG).show();

        // Finish the activity.
        finishAndRemoveTask();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountDecryptInProgress event) {
        Snackbar.make(_coordinatorLayout, getString(R.string.restore_account_snack_decrypting_backup),
                Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountDecryptCancelled event) {
        finishAndRemoveTask();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountDecryptComplete event) {

        if (event.getAccountData() != null) {

            final RestoreAccountModel viewModel = new ViewModelProvider(this)
                    .get(RestoreAccountModel.class);
            viewModel.getAccountDataLiveData().setValue(event.getAccountData());

            Snackbar.make(_coordinatorLayout, getString(R.string.restore_account_snack_decryption_successful),
                    Snackbar.LENGTH_SHORT).show();

        } else {

            if (event.isDecryptionError()) {
                // Shouldn't happen. Decryption errors are handled by the password dialog.
            } else if (event.isSerializationError()) {
                Snackbar.make(_coordinatorLayout,
                        getString(R.string.restore_account_snack_corrupt_backup),
                        Snackbar.LENGTH_INDEFINITE).addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        finishAndRemoveTask();
                    }
                }).show();
            }

        }

    }

}
