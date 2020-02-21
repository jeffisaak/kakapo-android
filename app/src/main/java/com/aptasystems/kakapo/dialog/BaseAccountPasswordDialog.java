package com.aptasystems.kakapo.dialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.view.View;
import android.widget.TextView;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.UserAccount;

import io.requery.query.Result;
import kakapo.crypto.exception.SignMessageException;
import kakapo.util.HashUtil;
import kakapo.util.StringUtil;

public abstract class BaseAccountPasswordDialog extends BaseDialog {

    protected static final String ARG_TITLE_STRING_ID = "titleStringId";

    private Long _userAccountId;

    private TextInputLayout _passwordTextInputLayout;
    private TextInputEditText _passwordEditText;

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _userAccountId = getArguments().getLong(ARG_USER_ACCOUNT_ID);

        _passwordTextInputLayout = view.findViewById(R.id.text_input_layout_password);
        _passwordEditText = view.findViewById(R.id.edit_text_password);

        TextView titleTextView = view.findViewById(R.id.text_view_enter_password_title);
        titleTextView.setText(getArguments().getInt(ARG_TITLE_STRING_ID));
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
            _passwordTextInputLayout.setError(getString(R.string.select_user_account_error_sign_in_no_password));
            return;
        }

        // Clear the error.
        _passwordTextInputLayout.setError(null);

        // Fetch the user account.
        Result<UserAccount> userAccountEntities = _entityStore.select(UserAccount.class)
                .where(UserAccount.ID.eq(_userAccountId))
                .get();
        UserAccount userAccount = userAccountEntities.first();

        // Hash the password that was entered.
        String hashedPassword = HashUtil.sha256ToString(password);

        // Check the entered password.
        try {
            _userAccountService.checkPassword(userAccount.getGuid(),
                    hashedPassword,
                    userAccount.getSecretKeyRings(),
                    userAccount.getPublicKeyRings());
        } catch (SignMessageException e) {
            // Wrong password.
            _passwordTextInputLayout.setError(getString(R.string.select_user_account_error_sign_in_wrong_password));
            return;
        }

        // Dismiss the dialog.
        dismiss();

        // Perform subclass-specific ok pressed stuff.
        okPressedInternal(userAccount, hashedPassword);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_enter_user_account_password;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }

    protected abstract void okPressedInternal(UserAccount userAccount, String hashedPassword);
}
