package com.aptasystems.kakapo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.aptasystems.kakapo.adapter.UserAccountRecyclerAdapter;
import com.aptasystems.kakapo.databinding.ActivitySelectUserAccountBinding;
import com.aptasystems.kakapo.dialog.CreateUserAccountDialog;
import com.aptasystems.kakapo.dialog.DeleteAccountDialog;
import com.aptasystems.kakapo.dialog.SignInDialog;
import com.aptasystems.kakapo.event.AccountCreationComplete;
import com.aptasystems.kakapo.event.AccountCreationInProgress;
import com.aptasystems.kakapo.event.AccountDeletionComplete;
import com.aptasystems.kakapo.event.AccountDeletionRequested;
import com.aptasystems.kakapo.event.AuthenticationComplete;
import com.aptasystems.kakapo.event.AuthenticationInProgress;
import com.aptasystems.kakapo.event.DownloadAccountComplete;
import com.aptasystems.kakapo.event.UserAccountListModelChanged;
import com.aptasystems.kakapo.event.UserAccountSelected;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.service.AccountBackupInfo;
import com.aptasystems.kakapo.service.AccountRestoreService;
import com.aptasystems.kakapo.service.AnnouncementService;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.view.FloatingMenu;
import com.aptasystems.kakapo.view.GenericDividerDecorator;
import com.aptasystems.kakapo.viewmodel.SelectUserAccountActivityModel;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.disposables.CompositeDisposable;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class SelectUserAccountActivity extends AppCompatActivity {

    private static final String SHOWCASE_ID = SelectUserAccountActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_CAMERA = 100;

    private static final int REQUEST_CAPTURE_QR_CODE = 100;

    @Inject
    AnnouncementService _announcementService;

    @Inject
    EventBus _eventBus;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    AccountRestoreService _accountRestoreService;

    @Inject
    PrefsUtil _prefsUtil;

    private FloatingMenu _floatingMenu;
    private UserAccountRecyclerAdapter _recyclerViewAdapter;
    private CompositeDisposable _compositeDisposable = new CompositeDisposable();
    private ActivitySelectUserAccountBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        _binding = ActivitySelectUserAccountBinding.inflate(getLayoutInflater());

        setContentView(_binding.getRoot());
        setSupportActionBar(_binding.toolbar);

        // Set up on clicks.
        _binding.addFloatingButton.setOnClickListener(this::toggleFloatingMenu);
        _binding.addNewAccountButton.setOnClickListener(this::addNewAccount);
        _binding.addFromAnotherDeviceButton.setOnClickListener(this::addFromAnotherDevice);
        _binding.addFromBackupButton.setOnClickListener(this::addFromBackup);

        // Set up the floating menu.
        _floatingMenu = new FloatingMenu.Builder()
                .withAddButton(_binding.addFloatingButton)
                .withExtraButton(_binding.addNewAccountButton,
                        _binding.addNewAccountLabel)
                .withExtraButton(_binding.addFromAnotherDeviceButton,
                        _binding.addFromAnotherDeviceLabel)
                .withExtraButton(_binding.addFromBackupButton,
                        _binding.addFromBackupLabel)
                .perItemTranslation(getResources().getDimension(R.dimen.fab_translate_per_item))
                .build();

        final SelectUserAccountActivityModel viewModel =
                new ViewModelProvider(this).get(SelectUserAccountActivityModel.class);
        viewModel.getFloatingMenuOpenLiveData().observe(this, isOpen -> {
            if (isOpen) {
                _floatingMenu.open(true);
            } else {
                _floatingMenu.close(true);
            }
        });

        // Set up the recycler view.
        _binding.includes.userAccountList.setHasFixedSize(true);
        _binding.includes.userAccountList.setLayoutManager(new LinearLayoutManager(this));
        _binding.includes.userAccountList.addItemDecoration(
                new GenericDividerDecorator(ContextCompat.getDrawable(this, R.drawable.divider_generic)));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new UserAccountRecyclerAdapter(this);

        _binding.includes.userAccountList.setAdapter(_recyclerViewAdapter);

        // Show any necessary announcments.
        _announcementService.showAnnouncements(getSupportFragmentManager());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register to listen for events.
        if (!_eventBus.isRegistered(this)) {
            _eventBus.register(this);
        }

        // Refresh the recycler view.
        _recyclerViewAdapter.refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();

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

    private void toggleFloatingMenu(View view) {
        final SelectUserAccountActivityModel viewModel = new ViewModelProvider(this)
                .get(SelectUserAccountActivityModel.class);
        boolean newValue = true;
        if (viewModel.getFloatingMenuOpenLiveData().getValue() != null) {
            newValue = !viewModel.getFloatingMenuOpenLiveData().getValue();
        }
        viewModel.getFloatingMenuOpenLiveData().setValue(newValue);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_select_user_account, menu);

        new Handler().post(() -> {
            ShowcaseConfig config = new ShowcaseConfig();
            config.setRenderOverNavigationBar(true);
            config.setDelay(100);
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID);
            sequence.setConfig(config);
            sequence.addSequenceItem(findViewById(R.id.action_help),
                    "Any time you see this button, you may tap it for help.", "GOT IT");
            sequence.addSequenceItem(_binding.addFloatingButton,
                    "Add or import user accounts with the add button.", "GOT IT");
            sequence.start();
        });

        return true;
    }

    public void showHelp(MenuItem menuItem) {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, R.raw.help_activity_select_user_account);
        startActivity(intent);
    }

    public void addNewAccount(View view) {

        final SelectUserAccountActivityModel viewModel = new ViewModelProvider(this)
                .get(SelectUserAccountActivityModel.class);
        viewModel.getFloatingMenuOpenLiveData().setValue(false);

        CreateUserAccountDialog dialog = CreateUserAccountDialog.newInstance();
        dialog.show(getSupportFragmentManager(), "createUserAccountDialog");
    }

    public void addFromAnotherDevice(View view) {

        final SelectUserAccountActivityModel viewModel = new ViewModelProvider(this)
                .get(SelectUserAccountActivityModel.class);
        viewModel.getFloatingMenuOpenLiveData().setValue(false);

        // Ensure we have camera permission.
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            openQrCodeScanner();
        }
    }

    private void openQrCodeScanner() {

        Toast.makeText(this,
                "Scan the QR code on your other device",
                Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, SimpleScannerActivity.class);
        startActivityForResult(intent, REQUEST_CAPTURE_QR_CODE);
    }

    public void addFromBackup(View view) {

        final SelectUserAccountActivityModel viewModel = new ViewModelProvider(this)
                .get(SelectUserAccountActivityModel.class);
        viewModel.getFloatingMenuOpenLiveData().setValue(false);

        // Show help.
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, R.raw.help_info_restore);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openQrCodeScanner();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CAPTURE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                final String scannedValue = data.getStringExtra(SimpleScannerActivity.EXTRA_SCANNED_VALUE);
                AccountBackupInfo accountBackupInfo =
                        AccountBackupInfo.from(scannedValue);

                // Go off to the server, download the account data, and import it.
                _compositeDisposable.add(
                        _accountRestoreService.downloadAccountShareAsync(accountBackupInfo));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UserAccountSelected event) {
        SignInDialog dialog = SignInDialog.newInstance(event.getUserAccountId(),
                R.string.select_user_account_dialog_title_sign_in);
        dialog.show(getSupportFragmentManager(), "signInDialog");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadAccountComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Refresh the list.
            _recyclerViewAdapter.refresh();

            // Let the user know that account creation was successful.
            Snackbar.make(_binding.coordinatorLayout,
                    R.string.select_user_account_snack_account_download_complete,
                    Snackbar.LENGTH_LONG).show();

        } else {


            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            switch (event.getStatus()) {
                case DecryptionFailed:
                    errorMessageId = R.string.restore_account_snack_error_account_download_decrypt_failed;
                    // FUTURE: Help link would be nice.
                    break;
                case AccountDeserializationFailed:
                    errorMessageId = R.string.restore_account_snack_error_account_download_deserialize_failed;
                    // FUTURE: Help link would be nice.
                    break;
                case NotFound:
                    errorMessageId = R.string.select_user_account_snack_error_account_download_not_found;
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    // FUTURE: Help link would be nice.
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
            }

            // Give the user a snack. Yum.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(SelectUserAccountActivity.this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            snackbar.show();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UserAccountListModelChanged event) {
        // Show/hide the recycler view and "no items" layout depending how many items are in
        // the model.
        _binding.includes.userAccountList.setVisibility(event.getNewItemCount() == 0 ? View.GONE : View.VISIBLE);
        _binding.includes.emptyListView.setVisibility(event.getNewItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountCreationInProgress event) {
        // Tell the user that account creation is in progress.
        Snackbar.make(_binding.coordinatorLayout,
                R.string.select_user_account_snack_account_creation_underway,
                Snackbar.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountCreationComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Refresh the list.
            _recyclerViewAdapter.refresh();

            // Let the user know that account creation was successful.
            Snackbar.make(_binding.coordinatorLayout,
                    R.string.select_user_account_snack_account_creation_complete,
                    Snackbar.LENGTH_LONG).show();

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            switch (event.getStatus()) {
                case KeyGenerationFailed:
                    errorMessageId = R.string.select_user_account_snack_error_key_ring_generation;
                    helpResId = R.raw.help_error_key_ring_generation;
                    break;
                case KeySerializationFailed:
                    errorMessageId = R.string.select_user_account_snack_error_key_ring_serialization;
                    helpResId = R.raw.help_error_key_ring_serialization;
                    break;
                case Unauthorized:
                    // Shouldn't happen, but we handle it if it does.
                    errorMessageId = R.string.select_user_account_snack_error_create_unauthorized;
                    break;
                case PayloadTooLarge:
                    errorMessageId = R.string.select_user_account_snack_key_rings_too_large;
                    helpResId = R.raw.help_error_public_key_too_large;
                    break;
                case InsufficientKeyLength:
                    // Shouldn't happen...
                    errorMessageId = R.string.select_user_account_snack_insufficient_key_length;
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
            }

            // Give the user a snack. Yum.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(SelectUserAccountActivity.this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            snackbar.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountDeletionRequested event) {
        int titleResId = event.isDeleteFromServer() ?
                R.string.select_user_account_dialog_title_delete_account_from_server :
                R.string.select_user_account_dialog_title_delete_account_from_device;
        DeleteAccountDialog dialog = DeleteAccountDialog
                .newInstance(event.getUserAccountId(), titleResId, event.isDeleteFromServer());
        dialog.show(getSupportFragmentManager(), "deleteAccountDialog");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountDeletionComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Refresh the list.
            _recyclerViewAdapter.refresh();

            // Let the user know that account deletion was successful.
            @StringRes int snackResId = event.isDeleteFromServer() ?
                    R.string.select_user_account_snack_account_deletion_from_server_complete :
                    R.string.select_user_account_snack_account_deletion_from_device_complete;
            Snackbar.make(_binding.coordinatorLayout, snackResId, Snackbar.LENGTH_LONG).show();

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            switch (event.getStatus()) {
                case IncorrectPassword:
                case Unauthorized:
                    errorMessageId = R.string.select_user_account_snack_error_unauthorized;
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
            }

            // Give the user a snack. Yum.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(SelectUserAccountActivity.this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            snackbar.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AuthenticationInProgress event) {
        Snackbar.make(_binding.coordinatorLayout,
                R.string.select_user_account_snack_authentication_underway,
                Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AuthenticationComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Put the guid and hashed password in the preferences.
            _prefsUtil.setCurrentUserAccountId(event.getUserAccountId());
            _prefsUtil.setCurrentHashedPassword(event.getHashedPassword());

            // Finish this activity and start the main activity.
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            switch (event.getStatus()) {
                case IncorrectPassword:
                case Unauthorized:
                    errorMessageId = R.string.select_user_account_snack_error_unauthorized;
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
            }

            // Give the user a snack. Yum.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(SelectUserAccountActivity.this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            snackbar.show();
        }
    }
}
