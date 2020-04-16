package com.aptasystems.kakapo.dialog;

import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.event.AccountCreationInProgress;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import kakapo.util.StringUtil;

public class CreateUserAccountDialog extends BaseDialog {

    private static final String TAG = CreateUserAccountDialog.class.getSimpleName();

    private TextInputLayout _accountNameTextInputLayout;
    private TextInputEditText _accountNameEditText;

    private TextInputLayout _passwordTextInputLayout;
    private TextInputEditText _passwordEditText;

    public static CreateUserAccountDialog newInstance() {
        return new CreateUserAccountDialog();
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _accountNameTextInputLayout = view.findViewById(R.id.text_input_layout_user_account_name);
        _accountNameEditText = view.findViewById(R.id.edit_text_user_account_name);

        _passwordTextInputLayout = view.findViewById(R.id.text_input_layout_password);
        _passwordEditText = view.findViewById(R.id.edit_text_password);
    }

    @Override
    protected void okPressed() {

        // Perform validation.
        boolean valid = true;
        String userAccountName = StringUtil.trimToNull(_accountNameEditText.getText().toString());
        if (userAccountName == null) {
            _accountNameTextInputLayout.setError(getString(R.string.select_user_account_error_create_no_name));
            valid = false;
        }
        String password = StringUtil.trimToNull(_passwordEditText.getText().toString());
        if (password == null) {
            _passwordTextInputLayout.setError(getString(R.string.select_user_account_error_create_no_password));
            valid = false;
        }
        if (!valid) {
            return;
        }

        // Clear the errors.
        _accountNameTextInputLayout.setError(null);
        _passwordTextInputLayout.setError(null);

        // Let the user know that account creation is underway. Post an event, the activity
        // will take care of the snack.
        _eventBus.post(new AccountCreationInProgress());

        // Start the background thread to create the new account. Will notify by event.
        _userAccountService.createNewAccountAsync(userAccountName, password);

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_create_user_account;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
