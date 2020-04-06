package com.aptasystems.kakapo.dialog;

import android.os.Bundle;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AuthenticationInProgress;

import androidx.annotation.StringRes;

public class SignInDialog extends BaseAccountPasswordDialog {

    public static SignInDialog newInstance(Long userAccountId, @StringRes int titleStringId) {
        SignInDialog result = new SignInDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ACCOUNT_ID, userAccountId);
        args.putInt(ARG_TITLE_STRING_ID, titleStringId);
        result.setArguments(args);
        return result;
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
    protected void okPressedInternal(UserAccount userAccount, String password) {

        // Let the user know that authentication is underway. Post an event, the activity
        // will take care of the snack.
        _eventBus.post(new AuthenticationInProgress());

        // Start the background thread to authenticate with the server.
        _userAccountService.authenticateAsync(userAccount, password);
    }
}
