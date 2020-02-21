package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.service.AccountData;
import com.aptasystems.kakapo.event.AccountDecryptCancelled;
import com.aptasystems.kakapo.event.AccountDecryptComplete;
import com.aptasystems.kakapo.event.AccountDecryptInProgress;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

import kakapo.crypto.exception.CryptoException;
import kakapo.util.StringUtil;

public class EnterRestorePasswordDialog extends BaseDialog {

    private static final String TAG = EnterRestorePasswordDialog.class.getSimpleName();

    private static final String ARG_ENCRYPTED_ACCOUNT_DATA = "encryptedAccountData";

    private byte[] _encryptedAccountData;

    private TextInputLayout _passwordTextInputLayout;
    private TextInputEditText _passwordEditText;

    public static EnterRestorePasswordDialog newInstance(byte[] encryptedAccountData) {
        EnterRestorePasswordDialog result = new EnterRestorePasswordDialog();
        Bundle args = new Bundle();
        args.putByteArray(ARG_ENCRYPTED_ACCOUNT_DATA, encryptedAccountData);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _encryptedAccountData = getArguments().getByteArray(ARG_ENCRYPTED_ACCOUNT_DATA);

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
            _passwordTextInputLayout.setError(getString(R.string.dialog_enter_restore_password_error_no_password));
            return;
        }

        // Clear the error.
        _passwordTextInputLayout.setError(null);

        // Post an event indicating we are starting the decrypt.
        _eventBus.post(new AccountDecryptInProgress());

        // Decrypt.
        try {
            AccountData accountData = _accountRestoreService.decryptAccountData(_encryptedAccountData, password);
            _eventBus.post(AccountDecryptComplete.success(accountData));
        } catch (CryptoException e) {
            // Password is probably wrong.
            _passwordTextInputLayout.setError(getString(R.string.dialog_enter_restore_password_error_incorrect_password));
            return;
        } catch (IOException e) {
            _eventBus.post(AccountDecryptComplete.serializationError());
        }

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected void cancelPressed() {
        _eventBus.post(new AccountDecryptCancelled());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_enter_restore_password;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
