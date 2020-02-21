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
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aptasystems.kakapo.HelpActivity;
import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.SelectUserAccountActivity;
import com.aptasystems.kakapo.SimpleScannerActivity;
import com.aptasystems.kakapo.adapter.FriendRecyclerAdapter;
import com.aptasystems.kakapo.dialog.AddFriendDialog;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.event.AddFriendComplete;
import com.aptasystems.kakapo.event.AddFriendInProgress;
import com.aptasystems.kakapo.event.FriendListModelChanged;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.view.FloatingActionLabel;
import com.aptasystems.kakapo.view.FloatingMenu;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.requery.query.Result;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.CLIPBOARD_SERVICE;

public class FriendListFragment extends BaseFragment {

    private static final String TAG = FriendListFragment.class.getSimpleName();
    private static final String SHOWCASE_ID = FriendListFragment.class.getSimpleName();

    private static final String STATE_KEY_FLOATING_MENU_OPEN = "floatingMenuOpen";

    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int REQUEST_CAPTURE_QR_CODE = 100;

    public static FriendListFragment newInstance() {
        FriendListFragment fragment = new FriendListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.showcase_view_anchor)
    FrameLayout _showcaseViewAnchor;

    @BindView(R.id.recycler_view_friend_list)
    RecyclerView _recyclerView;

    @BindView(R.id.fragment_friend_list_text_view_no_items)
    TextView _noItemsView;

    @BindView(R.id.floating_button_add_from_qr_code)
    FloatingActionButton _addFromQrCodeFloatingActionButton;

    @BindView(R.id.floating_label_add_from_qr_code)
    FloatingActionLabel _addFromQrCodeFloatingLabel;

    @BindView(R.id.floating_button_add_from_clipboard)
    FloatingActionButton _addFromClipboardFloatingActionButton;

    @BindView(R.id.floating_label_add_from_clipboard)
    FloatingActionLabel _addFromClipboardFloatingLabel;

    @BindView(R.id.floating_button_add_from_keyboard)
    FloatingActionButton _addFromKeyboardFloatingActionButton;

    @BindView(R.id.floating_label_add_from_keyboard)
    FloatingActionLabel _addFromKeyboardFloatingLabel;

    @BindView(R.id.floating_button_add)
    FloatingActionButton _addFloatingActionButton;

    private FloatingMenu _floatingMenu;
    private FriendRecyclerAdapter _recyclerViewAdapter;

    private boolean _floatingMenuOpen = false;

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
        View result = inflater.inflate(R.layout.fragment_friend_list, container, false);

        // If we don't have authentication info, just stop. The main activity will redirect us
        // to the sign in activity.
        if (_prefsUtil.getCurrentUserAccountId() == null && _prefsUtil.getCurrentHashedPassword() == null) {
            return result;
        }

        // Bind.
        _unbinder = ButterKnife.bind(this, result);

        // Set up the floating menu.
        _floatingMenu = new FloatingMenu.Builder().withAddButton(_addFloatingActionButton)
                .withExtraButton(_addFromQrCodeFloatingActionButton, _addFromQrCodeFloatingLabel)
                .withExtraButton(_addFromClipboardFloatingActionButton, _addFromClipboardFloatingLabel)
                .withExtraButton(_addFromKeyboardFloatingActionButton, _addFromKeyboardFloatingLabel)
                .perItemTranslation(getResources().getDimension(R.dimen.fab_translate_per_item))
                .build();

        // Saved instance state will be null if we have switched activities and then come back.
        // This is stupid, but means we need a workaround. To that end, we are setting some private
        // variables that will hold the state.
        if (savedInstanceState != null) {
            _floatingMenuOpen = savedInstanceState.getBoolean(STATE_KEY_FLOATING_MENU_OPEN);
        }

        // Set up the recycler view.
        _recyclerView.setHasFixedSize(true);
        _recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new FriendRecyclerAdapter(getActivity());
        _recyclerView.setAdapter(_recyclerViewAdapter);
        _recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));

        // If the floating menu was open, show it.
        if (_floatingMenuOpen) {
            _floatingMenu.open(false);
        }

        return result;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_KEY_FLOATING_MENU_OPEN, _floatingMenuOpen);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (_unbinder != null) {
            _unbinder.unbind();
        }
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

        new Handler().post(() -> {
            ShowcaseConfig config = new ShowcaseConfig();
            config.setRenderOverNavigationBar(true);
            config.setDelay(100);
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), SHOWCASE_ID);
            sequence.setConfig(config);
            sequence.addSequenceItem(_showcaseViewAnchor,
                    "This is your friends list. From here you can add and remove friends. Before you can share anything with Kakapo, you've got to have at least one friend to share it with.\n\nTo navigate to other parts of the app, swipe left or right or tap on the tab at the top.", "GOT IT");
            sequence.addSequenceItem(_addFloatingActionButton,
                    "Use the add button to add friends.",
                    "GOT IT");
            sequence.start();
        });
    }

    @OnClick(R.id.floating_button_add)
    public void expandFloatingMenu(View view) {
        if (!_floatingMenuOpen) {
            _floatingMenu.open(true);
            _floatingMenuOpen = true;
        } else {
            _floatingMenu.close(true);
            _floatingMenuOpen = false;
        }
    }

    @OnClick(R.id.floating_button_add_from_keyboard)
    public void addFromKeyboard(View view) {

        _floatingMenu.close(true);
        _floatingMenuOpen = false;

        // Open a dialog to allow the user to enter the name of the friend.
        AddFriendDialog dialog = AddFriendDialog.newInstance(
                _prefsUtil.getCurrentUserAccountId());
        dialog.show(getActivity().getSupportFragmentManager(), "addFriendDialog");
    }

    @OnClick(R.id.floating_button_add_from_clipboard)
    public void addFromClipboard(View view) {

        _floatingMenu.close(true);
        _floatingMenuOpen = false;

        // Get the ID from the clipboard.
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData == null) {
            Snackbar.make(_coordinatorLayout, R.string.fragment_friends_snack_nothing_in_clipboard, Snackbar.LENGTH_LONG)
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
            Snackbar.make(_coordinatorLayout, R.string.fragment_friends_snack_nothing_in_clipboard, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        // If we already have a friend with this GUID, we will not add it again.
        Result<Friend> FriendResult = _entityStore.select(Friend.class)
                .where(Friend.GUID.eq(clipText))
                .and(Friend.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .get();
        if (FriendResult.toList().size() > 0) {
            Snackbar.make(_coordinatorLayout, R.string.fragment_friends_snack_error_duplicate_friend_id, Snackbar.LENGTH_LONG).show();
            return;
        }

        // Open a dialog to allow the user to enter the name of the friend.
        AddFriendDialog dialog = AddFriendDialog.newInstance(
                _prefsUtil.getCurrentUserAccountId(),
                clipText);
        dialog.show(getActivity().getSupportFragmentManager(), "addFriendDialog");
    }

    @OnClick(R.id.floating_button_add_from_qr_code)
    public void addFromQrCode(View view) {

        _floatingMenu.close(true);
        _floatingMenuOpen = false;

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
        Intent intent = new Intent(getActivity(), SimpleScannerActivity.class);
        startActivityForResult(intent, REQUEST_CAPTURE_QR_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CAPTURE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                final String scannedValue = data.getStringExtra(SimpleScannerActivity.EXTRA_SCANNED_VALUE);

                // If we already have a friend with this GUID, we will not add it again.
                Result<Friend> FriendResult = _entityStore.select(Friend.class)
                        .where(Friend.GUID.eq(scannedValue))
                        .and(Friend.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                        .get();
                if (FriendResult.toList().size() > 0) {
                    Snackbar.make(_coordinatorLayout, R.string.fragment_friends_snack_error_duplicate_friend_id, Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Show the dialog box that allows the user to enter the name for the friend.
                // When they tap okay on that dialog box the server interaction to fetch the
                // public key occurs.
                new Handler().postDelayed(() -> {
                    AddFriendDialog dialog = AddFriendDialog.newInstance(
                            _prefsUtil.getCurrentUserAccountId(),
                            scannedValue);
                    dialog.show(getActivity().getSupportFragmentManager(), "addFriendDialog");
                }, 100);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendListModelChanged event) {
        _recyclerView.setVisibility(event.getNewItemCount() == 0 ? View.GONE : View.VISIBLE);
        _noItemsView.setVisibility(event.getNewItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final AddFriendInProgress event) {
        Snackbar.make(_recyclerView,
                R.string.fragment_friends_snack_add_friend_in_progress,
                Snackbar.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AddFriendComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Show a snack indicating the friend was added successfully and refresh the friend list.
            Snackbar.make(_coordinatorLayout, R.string.fragment_friends_snack_friend_successfully_added, Snackbar.LENGTH_LONG).show();
            _recyclerViewAdapter.refresh();

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