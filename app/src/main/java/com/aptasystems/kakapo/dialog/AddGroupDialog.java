package com.aptasystems.kakapo.dialog;

import android.os.Bundle;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.GroupAdded;

import kakapo.util.StringUtil;

public class AddGroupDialog extends BaseDialog {

    private static final String TAG = AddGroupDialog.class.getSimpleName();

    private TextInputLayout _groupNameTextInputLayout;
    private TextInputEditText _groupNameEditText;

    private long _userAccountId;

    public static AddGroupDialog newInstance(Long userAccountId) {
        AddGroupDialog result = new AddGroupDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ACCOUNT_ID, userAccountId);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _userAccountId = getArguments().getLong(ARG_USER_ACCOUNT_ID);

        _groupNameTextInputLayout = view.findViewById(R.id.text_input_layout_group_name);
        _groupNameEditText = view.findViewById(R.id.edit_text_group_name);
    }

    @Override
    protected void okPressed() {

        // Perform validation.
        boolean valid = true;
        String groupName = StringUtil.trimToNull(_groupNameEditText.getText().toString());
        if (groupName == null) {
            _groupNameTextInputLayout.setError(getString(R.string.dialog_add_group_error_create_no_name));
            valid = false;
        }
        if (!valid) {
            return;
        }

        // Clear the errors.
        _groupNameTextInputLayout.setError(null);

        // Fetch the user account.
        UserAccount userAccount = _userAccountDAO.find(_userAccountId);

        // Add the group.
        _groupDAO.insert(userAccount, groupName);

        // Post an event indicating the group was added.
        _eventBus.post(new GroupAdded());

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_add_group;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
