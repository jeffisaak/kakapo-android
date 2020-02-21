package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountDeletionComplete;
import com.aptasystems.kakapo.event.AccountDeletionInProgress;

import androidx.annotation.StringRes;

public class DeleteAccountDialog extends BaseAccountPasswordDialog {

    private static final String ARG_DELETE_FROM_SERVER = "deleteFromServer";

    private boolean _deleteFromServer;

    public static DeleteAccountDialog newInstance(Long userAccountId, @StringRes int titleStringId, boolean deleteFromServer) {
        DeleteAccountDialog result = new DeleteAccountDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ACCOUNT_ID, userAccountId);
        args.putInt(ARG_TITLE_STRING_ID, titleStringId);
        args.putBoolean(ARG_DELETE_FROM_SERVER, deleteFromServer);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        super.onCreateViewInternal(view);
        _deleteFromServer = getArguments().getBoolean(ARG_DELETE_FROM_SERVER);
        String title = getString(getArguments().getInt(ARG_TITLE_STRING_ID));

    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_enter_user_account_password;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }

    @Override
    protected void okPressedInternal(UserAccount userAccount, String hashedPassword) {

        if (_deleteFromServer) {
            // Post an event so that the user can be given feedback and start the
            // asynchronous deletion of the user account.
            _eventBus.post(new AccountDeletionInProgress());

            _userAccountService.deleteAccountFromServerAsync(userAccount, hashedPassword);

        } else {
            // Delete the account from the device and post an event to that effect.
            _userAccountService.deleteAccountFromDevice(userAccount);
            _eventBus.post(AccountDeletionComplete.success(_deleteFromServer));
        }
    }
}
