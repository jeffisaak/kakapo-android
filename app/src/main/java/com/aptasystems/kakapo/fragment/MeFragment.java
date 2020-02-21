package com.aptasystems.kakapo.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aptasystems.kakapo.BuildConfig;
import com.aptasystems.kakapo.HelpActivity;
import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.SelectUserAccountActivity;
import com.aptasystems.kakapo.adapter.QueuedItemRecyclerAdapter;
import com.aptasystems.kakapo.dialog.DeleteAccountDialog;
import com.aptasystems.kakapo.dialog.EnterBackupPasswordDialog;
import com.aptasystems.kakapo.dialog.RenameUserAccountDialog;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountBackupComplete;
import com.aptasystems.kakapo.event.AccountBackupInProgress;
import com.aptasystems.kakapo.event.AccountDeletionComplete;
import com.aptasystems.kakapo.event.AccountDeletionInProgress;
import com.aptasystems.kakapo.event.DeleteItemComplete;
import com.aptasystems.kakapo.event.QueuedItemsListModelChanged;
import com.aptasystems.kakapo.event.QuotaComplete;
import com.aptasystems.kakapo.event.SubmitItemComplete;
import com.aptasystems.kakapo.event.SubmitItemStarted;
import com.aptasystems.kakapo.event.UserAccountColourChanged;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.service.UserAccountService;
import com.aptasystems.kakapo.util.ColourUtil;
import com.aptasystems.kakapo.util.ShareUtil;
import com.aptasystems.kakapo.viewmodel.MeFragmentModel;
import com.google.android.material.snackbar.Snackbar;
import com.takisoft.colorpicker.ColorPickerDialog;
import com.takisoft.colorpicker.ColorStateDrawable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class MeFragment extends BaseFragment {

    private static final String TAG = MeFragment.class.getSimpleName();
    private static final String SHOWCASE_ID = MeFragment.class.getSimpleName();

    @Inject
    ColourUtil _colourUtil;

    @Inject
    UserAccountService _userAccountService;

    @BindView(R.id.showcase_view_anchor)
    FrameLayout _showcaseViewAnchor;

    @BindView(R.id.text_view_my_id)
    TextView _myIdTextView;

    @BindView(R.id.progress_bar_quota_usage)
    ProgressBar _quotaUsageProgressBar;

    @BindView(R.id.text_view_quota_detail)
    TextView _quotaDetailTextView;

    @BindView(R.id.avatar_colour_swatch)
    ImageView _avatarColourSwatchImageView;

    @BindView(R.id.image_button_quota_help)
    ImageButton _quotaHelpImageButton;

    @BindView(R.id.fragment_me_recycler_view_queued_items)
    RecyclerView _recyclerView;

    @BindView(R.id.fragment_me_text_view_no_queued_items)
    TextView _noItemsView;

    private ColorPickerDialog _colorPickerDialog;
    private QueuedItemRecyclerAdapter _recyclerViewAdapter;

    private CompositeDisposable _compositeDisposable = new CompositeDisposable();

    public static MeFragment newInstance() {
        MeFragment fragment = new MeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MeFragment() {
        // Required no argument public constructor.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ((KakapoApplication) getActivity().getApplication()).getKakapoComponent().inject(this);

        // Register to listen for events.
        if (!_eventBus.isRegistered(this)) {
            _eventBus.register(this);
        }
    }

    @Override
    public void onStart() {

        final MeFragmentModel viewModel = new ViewModelProvider(this)
                .get(MeFragmentModel.class);

        // Observe changes in the quota and update the UI.
        viewModel.getQuotaLiveData().observe(this, pair -> {

            if (pair != null) {

                _quotaUsageProgressBar.setIndeterminate(false);
                _quotaUsageProgressBar.setMax(pair.second);
                _quotaUsageProgressBar.setProgress(pair.first);

                String quotaText = String.format(getContext().
                        getString(R.string.fragment_me_quota_description), pair.first, pair.second);
                _quotaDetailTextView.setText(quotaText);

            } else {

                _quotaUsageProgressBar.setIndeterminate(false);
                _quotaDetailTextView.setText(getContext().getString(R.string.fragment_me_quota_unable));

            }
        });

        // Observe the swatch colour.
        viewModel.getSwatchColourLiveData().observe(this, colour -> {

            Drawable[] colourDrawable = new Drawable[]
                    {ContextCompat.getDrawable(getContext(), R.drawable.avatar_circle)};
            _avatarColourSwatchImageView.setImageDrawable(
                    new ColorStateDrawable(colourDrawable, colour));

        });

        // If the quota has not been fetched, set up the UI and go fetch the quota.
        if (viewModel.getQuotaLiveData().getValue() == null) {

            _quotaUsageProgressBar.setIndeterminate(true);
            _quotaDetailTextView.setText(getContext().
                    getString(R.string.fragment_me_quota_description_calculating));

            // Go off to the server and fetch quota.
            final UserAccount userAccount = _entityStore.findByKey(UserAccount.class,
                    _prefsUtil.getCurrentUserAccountId());

            Disposable disposable =
                    _userAccountService.getQuotaAsync(userAccount, _prefsUtil.getCurrentHashedPassword());
            _compositeDisposable.add(disposable);
        }

        super.onStart();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_me, container, false);

        // If we don't have authentication info, just stop. The main activity will redirect us
        // to the sign in activity.
        if (_prefsUtil.getCurrentUserAccountId() == null &&
                _prefsUtil.getCurrentHashedPassword() == null) {
            return result;
        }

        // Bind.
        _unbinder = ButterKnife.bind(this, result);

        // Set my ID.
        final UserAccount userAccount = _entityStore.findByKey(UserAccount.class, _prefsUtil.getCurrentUserAccountId());
        _myIdTextView.setText(userAccount.getGuid());

        // Set the colour swatch.
        final MeFragmentModel viewModel = new ViewModelProvider(this)
                .get(MeFragmentModel.class);
        viewModel.getSwatchColourLiveData().setValue(userAccount.getColour());

        // Click listener on the swatch opens a colour picker.
        _avatarColourSwatchImageView.setOnClickListener(v -> {
            UserAccount userAccount1 =
                    _entityStore.findByKey(UserAccount.class, _prefsUtil.getCurrentUserAccountId());
            _colorPickerDialog = _colourUtil.showColourPickerDialog(
                    getActivity(),
                    MeFragment.class,
                    userAccount1.getColour(), colour -> {
                        userAccount1.setColour(colour);
                        _entityStore.update(userAccount1);
                        _eventBus.post(new UserAccountColourChanged());
                    });
        });

        // Click listener on the quota help button opens help.
        _quotaHelpImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HelpActivity.class);
            intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, R.raw.help_info_quota);
            startActivity(intent);
        });

        // Set up the recycler view.
        _recyclerView.setHasFixedSize(true);
        _recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new QueuedItemRecyclerAdapter(getActivity());
        _recyclerView.setAdapter(_recyclerViewAdapter);
        _recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the list.
        _recyclerViewAdapter.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (_unbinder != null) {
            _unbinder.unbind();
        }

        if (_colorPickerDialog != null && _colorPickerDialog.isShowing()) {
            _colorPickerDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        _compositeDisposable.dispose();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_me, menu);

        new Handler().post(() -> {
            ShowcaseConfig config = new ShowcaseConfig();
            config.setRenderOverNavigationBar(true);
            config.setDelay(100);
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), SHOWCASE_ID);
            sequence.setConfig(config);
            sequence.addSequenceItem(_showcaseViewAnchor,
                    "This screen shows your quota and ID. You can share your ID, and backup, rename, or delete your account. ", "GOT IT");
            sequence.addSequenceItem(getActivity().findViewById(R.id.action_share),
                    "Use the share button to share your ID with friends or backup your account.",
                    "GOT IT");
            sequence.addSequenceItem(_avatarColourSwatchImageView,
                    "You can change the colour that your items are highlighted with.",
                    "GOT IT");
            sequence.start();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_delete_from_device: {
                DeleteAccountDialog dialog = DeleteAccountDialog.newInstance(
                        _prefsUtil.getCurrentUserAccountId(),
                        R.string.select_user_account_dialog_title_delete_account_from_device,
                        false);
                dialog.show(getActivity().getSupportFragmentManager(), "deleteAccountDialog");
                return true;
            }
            case R.id.action_delete_from_server: {
                DeleteAccountDialog dialog = DeleteAccountDialog.newInstance(
                        _prefsUtil.getCurrentUserAccountId(),
                        R.string.select_user_account_dialog_title_delete_account_from_server,
                        true);
                dialog.show(getActivity().getSupportFragmentManager(), "deleteAccountDialog");
                return true;
            }
            case R.id.action_backup: {
                EnterBackupPasswordDialog dialog = EnterBackupPasswordDialog.newInstance(_prefsUtil.getCurrentUserAccountId());
                dialog.show(getActivity().getSupportFragmentManager(), "enterBackupPasswordDialog");
                return true;
            }
            case R.id.action_rename: {
                RenameUserAccountDialog dialog = RenameUserAccountDialog.newInstance(_prefsUtil.getCurrentUserAccountId());
                dialog.show(getActivity().getSupportFragmentManager(), "renameUserAccountDialog");
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountDeletionInProgress event) {
        Snackbar.make(getView(), R.string.fragment_me_snack_deletion_in_progress, Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountDeletionComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Show a toast (toast because we are going to switch activities and we want it to
            // persist through the activity change).
            @StringRes int toastResId = event.isDeleteFromServer() ?
                    R.string.fragment_me_toast_account_deleted_from_server :
                    R.string.fragment_me_toast_account_deleted_from_device;
            Toast.makeText(getContext(), toastResId, Toast.LENGTH_LONG).show();

            // Finish this activity and start the activity to select the user account.
            Intent intent = new Intent(getActivity(), SelectUserAccountActivity.class);
            startActivity(intent);
            getActivity().finish();

            // It's important that we clear the credentials _after_ we have finished the activity,
            // as if the activity continues to run we will get problems in the tab fragments.
            _prefsUtil.clearCredentials();

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            boolean forceSignOut = false;
            switch (event.getStatus()) {
                case IncorrectPassword:
                case Unauthorized:
                    errorMessageId = R.string.app_snack_error_unauthorized;
                    forceSignOut = true;
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

            // Give the user a snack.
            Snackbar snackbar = Snackbar.make(getView(), errorMessageId, snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(getActivity(), HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            if (forceSignOut) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        _prefsUtil.clearCredentials();
                        Intent intent = new Intent(getActivity(), SelectUserAccountActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
            }
            snackbar.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(QuotaComplete event) {

        final MeFragmentModel viewModel = new ViewModelProvider(this)
                .get(MeFragmentModel.class);

        if (event.getStatus() == AsyncResult.Success) {
            viewModel.getQuotaLiveData().setValue(new Pair<>(event.getUsedQuota(), event.getMaxQuota()));
        } else {
            viewModel.getQuotaLiveData().setValue(null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UserAccountColourChanged event) {

        UserAccount userAccount =
                _entityStore.findByKey(UserAccount.class, _prefsUtil.getCurrentUserAccountId());

        final MeFragmentModel viewModel = new ViewModelProvider(this)
                .get(MeFragmentModel.class);
        viewModel.getSwatchColourLiveData().setValue(userAccount.getColour());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountBackupInProgress event) {
        Snackbar.make(getView(), R.string.fragment_me_snack_backup_in_progress,
                Snackbar.LENGTH_SHORT).show();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AccountBackupComplete event) {

        if (event.getEncryptedBackupFile() != null) {

            Intent shareIntent = ShareUtil.buildShareIntent(getContext(),
                    BuildConfig.APPLICATION_ID, event.getEncryptedBackupFile());
            Intent chooserIntent = Intent.createChooser(shareIntent,
                    getString(R.string.fragment_me_title_share_backup_file));
            if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(chooserIntent);
            } else {
                Snackbar.make(_coordinatorLayout,
                        R.string.fragment_me_snack_no_backup_share_apps,
                        Snackbar.LENGTH_LONG).show();
            }

        } else {

            // Show a generic error message.
            Snackbar.make(_coordinatorLayout, R.string.fragment_me_snack_error_creating_backup,
                    Snackbar.LENGTH_LONG).show();

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(QueuedItemsListModelChanged event) {
        // Show/hide the recycler view and "no items" layout depending how many items are in
        // the model.
        _recyclerView.setVisibility(event.getNewItemCount() == 0 ? View.GONE : View.VISIBLE);
        _noItemsView.setVisibility(event.getNewItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SubmitItemStarted event) {
        // Refresh the list of queued items.
        _recyclerViewAdapter.refresh();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SubmitItemComplete event) {

        // Don't filter based on event target as we want to update the quota after all submissions.

        // Only care about success here to update the quota widget.
        if (event.getStatus() == AsyncResult.Success) {

            // Update the quota widget.
            final MeFragmentModel viewModel = new ViewModelProvider(this)
                    .get(MeFragmentModel.class);
            viewModel.getQuotaLiveData().setValue(new Pair<>(event.getUsedQuota(), event.getMaxQuota()));

        }

        // Refresh the list of queued items.
        _recyclerViewAdapter.refresh();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DeleteItemComplete event) {
        // Only care about success here to update the quota widget.
        if (event.getStatus() == AsyncResult.Success) {

            // Update the quota widget.
            final MeFragmentModel viewModel = new ViewModelProvider(this)
                    .get(MeFragmentModel.class);
            viewModel.getQuotaLiveData().setValue(new Pair<>(event.getUsedQuota(), event.getMaxQuota()));

        }
    }
}