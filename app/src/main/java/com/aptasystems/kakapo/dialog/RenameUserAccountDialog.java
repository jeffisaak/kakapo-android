package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.UserAccountRenamed;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import kakapo.util.StringUtil;

public class RenameUserAccountDialog extends BaseDialog {

    private static final String TAG = RenameUserAccountDialog.class.getSimpleName();

    private TextInputLayout _accountNameTextInputLayout;
    private TextInputEditText _accountNameEditText;

    private long _userAccountId;

    public static RenameUserAccountDialog newInstance(Long userAccountId) {
        RenameUserAccountDialog result = new RenameUserAccountDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ACCOUNT_ID, userAccountId);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _userAccountId = getArguments().getLong(ARG_USER_ACCOUNT_ID);

        _accountNameTextInputLayout = view.findViewById(R.id.text_input_layout_user_account_name);
        _accountNameEditText = view.findViewById(R.id.edit_text_user_account_name);
    }

    @Override
    protected void okPressed() {

        String userAccountName = null;
        if (_accountNameEditText.getText() != null) {
            userAccountName = _accountNameEditText.getText().toString();
        }

        // Perform validation.
        boolean valid = true;
        userAccountName = StringUtil.trimToNull(userAccountName);
        if (userAccountName == null) {
            _accountNameTextInputLayout.setError(getString(R.string.select_user_account_error_create_no_name));
            valid = false;
        }
        if (!valid) {
            return;
        }

        // Clear the errors.
        _accountNameTextInputLayout.setError(null);

        // Rename the account.
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, _userAccountId);
        userAccount.setName(userAccountName);
        _entityStore.update(userAccount);

        // Post an event.
        _eventBus.post(new UserAccountRenamed(userAccountName));

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_rename_account;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
