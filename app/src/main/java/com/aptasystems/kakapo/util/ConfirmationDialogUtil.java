package com.aptasystems.kakapo.util;

import com.aptasystems.kakapo.dialog.ConfirmActionDialog;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

@Singleton
public class ConfirmationDialogUtil {

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    public ConfirmationDialogUtil() {
        // Noop.
    }

    public void showConfirmationDialog(FragmentManager fragmentManager,
                                       @StringRes int titleStringId,
                                       @StringRes int messageStringId,
                                       String dialogId,
                                       ConfirmActionDialog.OnProceedListener listener) {

        // First check the preferences to see if the user has already said "don't ask me again".
        // If they have, just call the listener and return.
        if( _prefsUtil.isDontAskAgain(dialogId)) {
            listener.proceed();
            return;
        }

        // Show the dialog.
        ConfirmActionDialog dialog = ConfirmActionDialog.newInstance(titleStringId,
                messageStringId,
                dialogId);
        dialog.setOnProceedListener(listener);
        dialog.show(fragmentManager, dialogId);
    }

}
