package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.event.FriendRenamed;

import kakapo.util.StringUtil;

public class RenameFriendDialog extends BaseDialog {

    private static final String TAG = RenameFriendDialog.class.getSimpleName();

    private static final String ARG_FRIEND_ID = "friendId";

    private TextInputLayout _friendNameTextInputLayout;
    private TextInputEditText _friendNameEditText;

    private long _friendId;

    public static RenameFriendDialog newInstance(Long friendId) {
        RenameFriendDialog result = new RenameFriendDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FRIEND_ID, friendId);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _friendId = getArguments().getLong(ARG_FRIEND_ID);

        _friendNameTextInputLayout = view.findViewById(R.id.text_input_layout_friend_name);
        _friendNameEditText = view.findViewById(R.id.edit_text_friend_name);
    }

    @Override
    protected void okPressed() {

        String friendName = null;
        if (_friendNameEditText.getText() != null) {
            friendName = _friendNameEditText.getText().toString();
        }

        // Perform validation.
        boolean valid = true;
        friendName = StringUtil.trimToNull(friendName);
        if (friendName == null) {
            _friendNameTextInputLayout.setError(getString(R.string.dialog_rename_friend_error_create_no_name));
            valid = false;
        }
        if (!valid) {
            return;
        }

        // Clear the errors.
        _friendNameTextInputLayout.setError(null);

        // Rename the friend.
        _friendDAO.updateName(_friendId, friendName);

        // Post an event.
        Friend friend = _friendDAO.find(_friendId);
        _eventBus.post(new FriendRenamed(friend.getGuid(), friend.getName()));

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_rename_friend;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
