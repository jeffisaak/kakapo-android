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

public class EnterDownloadAccountPasswordDialog extends BaseDialog {

    private static final String TAG = EnterDownloadAccountPasswordDialog.class.getSimpleName();

    private static final String ARG_ACCOUNT_BACKUP_INFO_STRING = "accountBackupInfoString";

    private String _accountBackupInfoString;

    private TextInputLayout _passwordTextInputLayout;
    private TextInputEditText _passwordEditText;

    public static EnterDownloadAccountPasswordDialog newInstance(String accountBackupInfoString) {
        EnterDownloadAccountPasswordDialog result = new EnterDownloadAccountPasswordDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT_BACKUP_INFO_STRING, accountBackupInfoString);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _accountBackupInfoString = getArguments().getString(ARG_ACCOUNT_BACKUP_INFO_STRING);

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

        // Create a new user account.
        AccountBackupInfo accountBackupInfo = AccountBackupInfo.from(_accountBackupInfoString);
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

            dismiss();

            _eventBus.post(DownloadAccountComplete.success());

        } else {
            // Delete the (partial) user account we created.
            UserAccount userAccount = _userAccountDAO.find(event.getUserAccountId());
            _userAccountDAO.delete(userAccount);

            // Show an error.
            _passwordTextInputLayout.setError(getString(R.string.select_user_account_error_sign_in_wrong_password));
        }
    }

    @Override
    protected void cancelPressed() {
        _eventBus.post(new AccountDecryptCancelled());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_enter_download_account_password;
    }

    @Override
    protected boolean usesEventBus() {
        return true;
    }
}
