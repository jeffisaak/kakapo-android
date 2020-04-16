package com.aptasystems.kakapo;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.aptasystems.kakapo.adapter.FriendGroupListAdapter;
import com.aptasystems.kakapo.adapter.model.FriendGroupListItem;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.GroupDAO;
import com.aptasystems.kakapo.dao.GroupMemberDAO;
import com.aptasystems.kakapo.databinding.ActivityFriendDetailBinding;
import com.aptasystems.kakapo.dialog.RenameFriendDialog;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.event.FriendColourChanged;
import com.aptasystems.kakapo.event.FriendRenamed;
import com.aptasystems.kakapo.service.FriendService;
import com.aptasystems.kakapo.util.ColourUtil;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.util.ShareUtil;
import com.google.android.material.snackbar.Snackbar;
import com.takisoft.colorpicker.ColorPickerDialog;
import com.takisoft.colorpicker.ColorStateDrawable;

import net.glxn.qrgen.android.QRCode;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class FriendDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FRIEND_ID = "friendId";

    @Inject
    FriendDAO _friendDAO;

    @Inject
    GroupDAO _groupDAO;

    @Inject
    GroupMemberDAO _groupMemberDAO;

    @Inject
    ColourUtil _colourUtil;

    @Inject
    EventBus _eventBus;

    @Inject
    FriendService _friendService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    private FriendGroupListAdapter _listAdapter;
    private ColorPickerDialog _colourPickerDialog;
    private ActivityFriendDetailBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        _binding = ActivityFriendDetailBinding.inflate(getLayoutInflater());

        setContentView(_binding.getRoot());
        setSupportActionBar(_binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        updateUserInterface();

        final Long friendId = getIntent().getLongExtra(EXTRA_FRIEND_ID, 0L);

        // Click listener on the swatch opens a colour picker.
        _binding.includes.avatarColourSwatch.setOnClickListener(v -> {
            Friend friend = _friendDAO.find(friendId);
            _colourPickerDialog = _colourUtil.showColourPickerDialog(
                    FriendDetailActivity.this,
                    FriendDetailActivity.class,
                    friend.getColour(), colour -> {
                        _friendDAO.updateColour(friendId, colour);
                        _eventBus.post(new FriendColourChanged(friendId, colour));
                    });
        });

        // Set up the list view.
        _listAdapter = new FriendGroupListAdapter(this, friendId);
        _binding.includes.friendGroupsList.setAdapter(_listAdapter);
        _binding.includes.friendGroupsList.setOnItemClickListener((parent, view, position, id) -> {
            FriendGroupListItem item = _listAdapter.getItem(position);
            item.setMember(!item.isMember());
            _listAdapter.notifyDataSetChanged();

            Friend friend = _friendDAO.find(item.getFriendId());
            Group group = _groupDAO.find(item.getGroupId());

            if (item.isMember()) {
                _groupMemberDAO.insert(friend, group);
            } else {
                _groupMemberDAO.delete(friend, group);
            }
        });
        _binding.includes.friendGroupsList.setEmptyView(_binding.includes.emptyListView);
        _listAdapter.refresh();
    }

    private void updateUserInterface() {

        // Set the title to the name of the friend.
        Long friendId = getIntent().getLongExtra(EXTRA_FRIEND_ID, 0L);
        final Friend friend = _friendDAO.find(friendId);
        String title = String.format(getString(R.string.friend_detail_title_activity), friend.getName());
        setTitle(title);

        // Set the friend GUID display value.
        _binding.includes.friendGuid.setText(friend.getGuid());

        // Set up the colour swatch.
        Drawable[] colourDrawable = new Drawable[]
                {ContextCompat.getDrawable(this, R.drawable.avatar_circle)};
        _binding.includes.avatarColourSwatch.setImageDrawable(new ColorStateDrawable(colourDrawable, friend.getColour()));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Dismiss the colour picker dialog if it is showing.
        if (_colourPickerDialog != null && _colourPickerDialog.isShowing()) {
            _colourPickerDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_friend_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendColourChanged event) {
        updateUserInterface();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendRenamed event) {
        updateUserInterface();
    }

    /**
     * Share the friend's ID as a QR code. Invoked by a menu item.
     *
     * @param menuItem
     */
    public void shareIdAsQrCode(MenuItem menuItem) {

        final Long friendId = getIntent().getLongExtra(EXTRA_FRIEND_ID, 0L);
        Friend friend = _friendDAO.find(friendId);

        // Generate the QR code bitmap.
        int pixelSize = getResources().getDimensionPixelSize(R.dimen.qr_code_size);
        Bitmap qrCodeBitmap = QRCode.from(friend.getGuid()).withSize(pixelSize, pixelSize).bitmap();

        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(dialogInterface -> {
            // Noop.
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(qrCodeBitmap);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.show();
    }

    /**
     * Share the friend's ID as text. Invoked by a menu item.
     *
     * @param menuItem
     */
    public void shareIdAsText(MenuItem menuItem) {

        final Long friendId = getIntent().getLongExtra(EXTRA_FRIEND_ID, 0L);
        Friend friend = _friendDAO.find(friendId);

        Intent shareIntent = ShareUtil.buildShareIntent(friend.getGuid());
        Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.app_title_share_id_with));
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            Snackbar.make(_binding.coordinatorLayout,
                    R.string.app_snack_error_no_id_share_targets,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    public void renameFriend(MenuItem menuItem) {
        final Long friendId = getIntent().getLongExtra(EXTRA_FRIEND_ID, 0L);
        RenameFriendDialog dialog = RenameFriendDialog.newInstance(friendId);
        dialog.show(getSupportFragmentManager(), "renameFriendDialog");
    }

    public void deleteFriend(MenuItem menuItem) {

        _confirmationDialogUtil.showConfirmationDialog(getSupportFragmentManager(),
                R.string.dialog_confirm_title_delete_friend,
                R.string.dialog_confirm_text_delete_friend,
                "deleteFriendConfirmation",
                () -> {
                    // Delete the friend.
                    final Long friendId = getIntent().getLongExtra(EXTRA_FRIEND_ID, 0L);
                    Friend friend = _friendDAO.find(friendId);
                    _friendService.deleteFriend(friend);

                    // Finish the activity.
                    finish();
                });

    }
}
