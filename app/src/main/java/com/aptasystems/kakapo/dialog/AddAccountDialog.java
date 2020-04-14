package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountDecryptCancelled;
import com.aptasystems.kakapo.event.AccountDecryptInProgress;
import com.aptasystems.kakapo.event.DownloadAccountComplete;
import com.aptasystems.kakapo.event.RestoreRemoteBackupComplete;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.service.AccountBackupInfo;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import kakapo.util.StringUtil;

public class AddAccountDialog extends BaseDialog {

    private static final String ARG_ACCOUNT_BACKUP_INFO_STRING = "accountBackupInfoString";

    private TextInputLayout _accountInfoStringTextInputLayout;
    private TextInputEditText _accountInfoStringEditText;
    private TextInputLayout _passwordTextInputLayout;
    private TextInputEditText _passwordEditText;

    public static AddAccountDialog newInstance(String accountBackupInfoString) {
        AddAccountDialog result = new AddAccountDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT_BACKUP_INFO_STRING, accountBackupInfoString);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _accountInfoStringTextInputLayout = view.findViewById(R.id.text_input_layout_backup_string);
        _accountInfoStringEditText = view.findViewById(R.id.edit_text_backup_string);

        _passwordTextInputLayout = view.findViewById(R.id.text_input_layout_password);
        _passwordEditText = view.findViewById(R.id.edit_text_password);

        String accountBackupInfoString = getArguments().getString(ARG_ACCOUNT_BACKUP_INFO_STRING, "");
        _accountInfoStringEditText.setText(accountBackupInfoString);
    }

    @Override
    protected void okPressed() {

        String accountBackupInfoString = null;
        if (_accountInfoStringEditText.getText() != null) {
            accountBackupInfoString = StringUtil.trimToNull(_accountInfoStringEditText.getText().toString());
        }

        String password = null;
        if (_passwordEditText.getText() != null) {
            password = StringUtil.trimToNull(_passwordEditText.getText().toString());
        }

        // Perform validation.

        if (accountBackupInfoString == null) {
            _accountInfoStringTextInputLayout.setError("Please enter a value");
            _passwordTextInputLayout.setError(null);
            return;
        } else {
            AccountBackupInfo accountBackupInfo = AccountBackupInfo.from(accountBackupInfoString);
            if (accountBackupInfo == null) {
                _accountInfoStringTextInputLayout.setError("Invalid account backup info");
                _passwordTextInputLayout.setError(null);
                return;
            }
        }

        if (password == null) {
            _accountInfoStringTextInputLayout.setError(null);
            _passwordTextInputLayout.setError(getString(R.string.dialog_enter_restore_password_error_no_password));
            return;
        }

        // Clear the error.
        _accountInfoStringTextInputLayout.setError(null);
        _passwordTextInputLayout.setError(null);

        // Post an event indicating we are starting the decrypt.
        _eventBus.post(new AccountDecryptInProgress());

        // Create a new user account.
        AccountBackupInfo accountBackupInfo = AccountBackupInfo.from(accountBackupInfoString);
        UserAccount userAccount = new UserAccount();
        userAccount.setGuid(accountBackupInfo.getGuid());
        userAccount.setPasswordSalt(accountBackupInfo.getPasswordSalt());
        userAccount.setApiKey(accountBackupInfo.getApiKey());
        userAccount.setBackupRequired(false);
        _userAccountDAO.insert(userAccount);

        // Start the download and decrypt and stuff. Event listeners will close the dialog.
        _accountRestoreService.checkAndMergeRemoteBackupAsync(userAccount.getId(), password);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(RestoreRemoteBackupComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // If restore was successful, dismiss the dialog and post another event.
            dismiss();
            _eventBus.post(DownloadAccountComplete.success());

        } else {

            // If restore was not successful, delete the partial user account we created and
            // show an error message.

            // Delete the (partial) user account we created.
            UserAccount userAccount = _userAccountDAO.find(event.getUserAccountId());
            _userAccountDAO.delete(userAccount);

            System.out.println("[AAA] " + event.getStatus());

            // Show an error.
            switch (event.getStatus()) {
                case RetrofitIOException:
                    _passwordTextInputLayout.setError("Unable to connect to the Kakapo server");
                    break;
                case BadRequest:
                    // Shouldn't happen...
                    _passwordTextInputLayout.setError("An error occurred while communicating with the server");
                    break;
                case ServerUnavailable:
                    _passwordTextInputLayout.setError("Unable to connect to the Kakapo server");
                    break;
                case TooManyRequests:
                    _passwordTextInputLayout.setError("Too many requests; please wait a moment and try again");
                    break;
                case OtherHttpError:
                    _passwordTextInputLayout.setError("An error occurred while communicating with the server");
                    break;
                case Unauthorized:
                case DecryptionFailed:
                    _passwordTextInputLayout.setError(getString(R.string.select_user_account_error_sign_in_wrong_password));
                    break;
                case NotFound:
                    _passwordTextInputLayout.setError("Unable to find remote backup");
                    break;
                case ContentStreamFailed:
                    _passwordTextInputLayout.setError("An error occurred downloading account data; please try again");
                    break;
                case AccountDeserializationFailed:
                    _passwordTextInputLayout.setError("Unable to read downloaded account data");
                    break;
            }
        }
    }

    @Override
    protected void cancelPressed() {
        _eventBus.post(new AccountDecryptCancelled());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_add_account;
    }

    @Override
    protected boolean usesEventBus() {
        return true;
    }
}
