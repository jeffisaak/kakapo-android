package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.event.AccountBackupInProgress;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import kakapo.util.StringUtil;

public class EnterBackupPasswordDialog extends BaseDialog {

    private static final String TAG = EnterBackupPasswordDialog.class.getSimpleName();

    private long _userAccountId;

    private TextInputLayout _passwordTextInputLayout;
    private TextInputEditText _passwordEditText;

    public static EnterBackupPasswordDialog newInstance(Long userAccountId) {
        EnterBackupPasswordDialog result = new EnterBackupPasswordDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ACCOUNT_ID, userAccountId);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _userAccountId = getArguments().getLong(ARG_USER_ACCOUNT_ID);

        _passwordTextInputLayout = view.findViewById(R.id.text_input_layout_password);
        _passwordEditText = view.findViewById(R.id.edit_text_password);
    }

    @Override
    protected void okPressed() {

        String password = null;
        if (_passwordEditText.getText() != null) {
            password = _passwordEditText.getText().toString();
        }

        // Perform validation.
        password = StringUtil.trimToNull(password);
        if (password == null) {
            _passwordTextInputLayout.setError(getString(R.string.dialog_enter_backup_password_error_sign_in_no_password));
            return;
        }

        // Clear the error.
        _passwordTextInputLayout.setError(null);

        // Post an event indicating we are starting the backup.
        _eventBus.post(new AccountBackupInProgress());

        // Start the backup.
        _accountBackupService.createAccountBackupAsync(_userAccountId, password);

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_enter_backup_password;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }

}
