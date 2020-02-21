package com.aptasystems.kakapo.dialog;

import android.os.Bundle;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.event.AddFriendInProgress;

import kakapo.util.StringUtil;

public class AddFriendDialog extends BaseDialog {

    private static final String ARG_FRIEND_GUID = "friendGuid";

    private TextInputLayout _friendNameTextInputLayout;
    private TextInputEditText _friendNameEditText;
    private TextInputLayout _friendGuidTextInputLayout;
    private TextInputEditText _friendGuidEditText;

    private long _userAccountRid;

    public static AddFriendDialog newInstance(Long userAccountId, String friendGuid) {
        AddFriendDialog result = new AddFriendDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ACCOUNT_ID, userAccountId);
        if( friendGuid != null ) {
            args.putString(ARG_FRIEND_GUID, friendGuid);
        }
        result.setArguments(args);
        return result;
    }

    public static AddFriendDialog newInstance(Long userAccountId) {
        return newInstance(userAccountId, null);
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        if (getArguments() != null) {
            _userAccountRid = getArguments().getLong(ARG_USER_ACCOUNT_ID);
        }

        _friendNameTextInputLayout = view.findViewById(R.id.text_input_layout_friend_name);
        _friendNameEditText = view.findViewById(R.id.edit_text_friend_name);

        _friendGuidTextInputLayout = view.findViewById(R.id.text_input_layout_friend_guid);
        _friendGuidEditText = view.findViewById(R.id.edit_text_friend_guid);
        _friendGuidEditText.setText(getArguments().getString(ARG_FRIEND_GUID, ""));
    }

    @Override
    protected void okPressed() {

        String friendName = null;
        if (_friendNameEditText.getText() != null) {
            friendName = _friendNameEditText.getText().toString();
        }

        String friendGuid = null;
        if (_friendGuidEditText.getText() != null) {
            friendGuid = _friendGuidEditText.getText().toString();
        }

        // Perform validation.

        boolean valid = true;

        friendName = StringUtil.trimToNull(friendName);
        if (friendName == null) {
            _friendNameTextInputLayout.setError(getString(R.string.dialog_add_friend_error_create_no_name));
            valid = false;
        }
        friendGuid = StringUtil.trimToNull(friendGuid);
        if (friendGuid == null) {
            _friendGuidTextInputLayout.setError(getString(R.string.dialog_add_friend_error_create_no_guid));
            valid = false;
        }
        if (!valid) {
            return;
        }

        // Clear the errors.
        _friendNameTextInputLayout.setError(null);
        _friendGuidTextInputLayout.setError(null);

        // Post an event indicating we are adding a friend.
        _eventBus.post(new AddFriendInProgress());

        // Add the friend.
        _friendService.addFriendAsync(_userAccountRid,
                _prefsUtil.getCurrentHashedPassword(),
                friendName,
                friendGuid);

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_add_friend;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
