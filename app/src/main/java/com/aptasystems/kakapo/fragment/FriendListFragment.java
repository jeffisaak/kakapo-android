package com.aptasystems.kakapo.fragment;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aptasystems.kakapo.HelpActivity;
import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.SelectUserAccountActivity;
import com.aptasystems.kakapo.adapter.FriendRecyclerAdapter;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.databinding.FragmentFriendListBinding;
import com.aptasystems.kakapo.dialog.AddFriendDialog;
import com.aptasystems.kakapo.dialog.EnterDownloadAccountPasswordDialog;
import com.aptasystems.kakapo.dialog.ScanQRCodeDialog;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.event.AddFriendComplete;
import com.aptasystems.kakapo.event.AddFriendInProgress;
import com.aptasystems.kakapo.event.FriendListModelChanged;
import com.aptasystems.kakapo.event.FriendListUpdated;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.service.AccountBackupInfo;
import com.aptasystems.kakapo.view.FloatingMenu;
import com.aptasystems.kakapo.viewmodel.FriendListFragmentModel;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.requery.query.Result;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.CLIPBOARD_SERVICE;

public class FriendListFragment extends BaseFragment {

    private static final String TAG = FriendListFragment.class.getSimpleName();
    private static final String SHOWCASE_ID = FriendListFragment.class.getSimpleName();

    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int REQUEST_CAPTURE_QR_CODE = 100;

    @Inject
    FriendDAO _friendDAO;

    public static FriendListFragment newInstance() {
        FriendListFragment fragment = new FriendListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private FloatingMenu _floatingMenu;
    private FriendRecyclerAdapter _recyclerViewAdapter;
    private FragmentFriendListBinding _binding;

    public FriendListFragment() {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        _binding = FragmentFriendListBinding.inflate(inflater, container, false);

        // If we don't have authentication info, just stop. The main activity will redirect us
        // to the sign in activity.
        if (_prefsUtil.getCurrentUserAccountId() == null && _prefsUtil.getCurrentPassword() == null) {
            return _binding.getRoot();
        }

        // On click listeners.
        _binding.addFloatingButton.setOnClickListener(this::toggleFloatingMenu);
        _binding.addFromClipboardButton.setOnClickListener(this::addFromClipboard);
        _binding.addFromQrCodeButton.setOnClickListener(this::addFromQrCode);
        _binding.addFromKeyboardButton.setOnClickListener(this::addFromKeyboard);

        // Set up the floating menu.
        _floatingMenu = new FloatingMenu.Builder()
                .withAddButton(_binding.addFloatingButton)
                .withExtraButton(_binding.addFromQrCodeButton,
                        _binding.addFromQrCodeLabel)
                .withExtraButton(_binding.addFromClipboardButton,
                        _binding.addFromClipboardLabel)
                .withExtraButton(_binding.addFromKeyboardButton,
                        _binding.addFromKeyboardLabel)
                .perItemTranslation(getResources().getDimension(R.dimen.fab_translate_per_item))
                .build();

        // Set up the recycler view.
        _binding.friendList.setHasFixedSize(true);
        _binding.friendList.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new FriendRecyclerAdapter(getActivity());
        _binding.friendList.setAdapter(_recyclerViewAdapter);
        _binding.friendList.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));

        return _binding.getRoot();
    }

    @Override
    public void onStart() {

        final FriendListFragmentModel viewModel = new ViewModelProvider(this)
                .get(FriendListFragmentModel.class);

        viewModel.getFloatingMenuOpenLiveData().observe(this, isOpen -> {
            if (isOpen) {
                _floatingMenu.open(true);
            } else {
                _floatingMenu.close(true);
            }
        });

        super.onStart();
    }

    @Override
    public void onDestroyView() {
        _binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the list.
        _recyclerViewAdapter.refresh();

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        boolean skipTutorial = getResources().getBoolean(R.bool.skip_showcase_tutorial);
        if (!skipTutorial) {
            new Handler().post(() -> {
                ShowcaseConfig config = new ShowcaseConfig();
                config.setRenderOverNavigationBar(true);
                config.setDelay(100);
                MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), SHOWCASE_ID);
                sequence.setConfig(config);
                sequence.addSequenceItem(_binding.showcaseViewAnchor,
                        "This is your friends list. From here you can add and remove friends. Before you can share anything with Kakapo, you've got to have at least one friend to share it with.\n\nTo navigate to other parts of the app, swipe left or right or tap on the tab at the top.", "GOT IT");
                sequence.addSequenceItem(_binding.addFloatingButton,
                        "Use the add button to add friends.",
                        "GOT IT");
                sequence.start();
            });
        }
    }

    private void toggleFloatingMenu(View view) {
        final FriendListFragmentModel viewModel = new ViewModelProvider(this)
                .get(FriendListFragmentModel.class);
        boolean newValue = true;
        if (viewModel.getFloatingMenuOpenLiveData().getValue() != null) {
            newValue = !viewModel.getFloatingMenuOpenLiveData().getValue();
        }
        viewModel.getFloatingMenuOpenLiveData().setValue(newValue);
    }

    private void addFromKeyboard(View view) {

        final FriendListFragmentModel viewModel = new ViewModelProvider(this)
                .get(FriendListFragmentModel.class);
        viewModel.getFloatingMenuOpenLiveData().setValue(false);

        // Open a dialog to allow the user to enter the name of the friend.
        AddFriendDialog dialog = AddFriendDialog.newInstance(
                _prefsUtil.getCurrentUserAccountId());
        dialog.show(getActivity().getSupportFragmentManager(), "addFriendDialog");
    }

    private void addFromClipboard(View view) {

        final FriendListFragmentModel viewModel = new ViewModelProvider(this)
                .get(FriendListFragmentModel.class);
        viewModel.getFloatingMenuOpenLiveData().setValue(false);

        // Get the ID from the clipboard.
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData == null) {
            Snackbar.make(_binding.coordinatorLayout, R.string.fragment_friends_snack_nothing_in_clipboard, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        String clipText = null;
        for (int ii = 0; ii < clipData.getItemCount(); ii++) {
            ClipData.Item item = clipData.getItemAt(ii);
            if (item.getText() != null) {
                clipText = item.getText().toString();
                break;
            }
        }

        if (clipText == null) {
            Snackbar.make(_binding.coordinatorLayout, R.string.fragment_friends_snack_nothing_in_clipboard, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        // If we already have a friend with this GUID, we will not add it again.
        Friend friend = _friendDAO.find(_prefsUtil.getCurrentUserAccountId(), clipText);
        if (friend != null) {
            Snackbar.make(_binding.coordinatorLayout, R.string.fragment_friends_snack_error_duplicate_friend_id, Snackbar.LENGTH_LONG).show();
            return;
        }

        // Open a dialog to allow the user to enter the name of the friend.
        AddFriendDialog dialog = AddFriendDialog.newInstance(
                _prefsUtil.getCurrentUserAccountId(),
                clipText);
        dialog.show(getActivity().getSupportFragmentManager(), "addFriendDialog");
    }

    public void addFromQrCode(View view) {

        final FriendListFragmentModel viewModel = new ViewModelProvider(this)
                .get(FriendListFragmentModel.class);
        viewModel.getFloatingMenuOpenLiveData().setValue(false);

        // Ensure we have camera permission.
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            openQrCodeScanner();
        }
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

    private void openQrCodeScanner() {
        // Set up and show the scan QR code dialog.
        ScanQRCodeDialog dialog = ScanQRCodeDialog.newInstance(R.string.dialog_scan_instructions_add_friend);
        dialog.setValidator(qrCode -> {
            return true;
        });
        dialog.setResultHandler(qrCode -> {

            // If we already have a friend with this GUID, we will not add it again.
            Friend friend = _friendDAO.find(_prefsUtil.getCurrentUserAccountId(), qrCode);
            if (friend != null) {
                Snackbar.make(_binding.coordinatorLayout, R.string.fragment_friends_snack_error_duplicate_friend_id, Snackbar.LENGTH_LONG).show();
                return;
            }

            // Show the dialog box that allows the user to enter the name for the friend.
            // When they tap okay on that dialog box the server interaction to fetch the
            // public key occurs.
            AddFriendDialog dialog1 = AddFriendDialog.newInstance(
                    _prefsUtil.getCurrentUserAccountId(),
                    qrCode);
            dialog1.show(getActivity().getSupportFragmentManager(), "addFriendDialog");
        });
        dialog.show(getActivity().getSupportFragmentManager(), "scanQrCodeDialog");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendListUpdated event) {
        _recyclerViewAdapter.refresh();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendListModelChanged event) {
        _binding.friendList.setVisibility(event.getNewItemCount() == 0 ? View.GONE : View.VISIBLE);
        _binding.emptyListView.setVisibility(event.getNewItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final AddFriendInProgress event) {
        Snackbar.make(_binding.coordinatorLayout,
                R.string.fragment_friends_snack_add_friend_in_progress,
                Snackbar.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AddFriendComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Show a snack indicating the friend was added successfully and refresh the friend list.
            Snackbar.make(_binding.coordinatorLayout, R.string.fragment_friends_snack_friend_successfully_added, Snackbar.LENGTH_LONG).show();
            _recyclerViewAdapter.refresh();

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            boolean forceSignOut = false;
            switch (event.getStatus()) {
                case BadRequest:
                    errorMessageId = R.string.app_snack_error_bad_request;
                    break;
                case Unauthorized:
                    errorMessageId = R.string.app_snack_error_unauthorized;
                    forceSignOut = true;
                    break;
                case NotFound:
                    errorMessageId = R.string.fragment_friends_snack_error_guid_not_found;
                    helpResId = R.raw.help_error_friend_guid_not_found;
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
}