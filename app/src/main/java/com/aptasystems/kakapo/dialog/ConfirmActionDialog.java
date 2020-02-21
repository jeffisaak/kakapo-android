package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.aptasystems.kakapo.R;

import androidx.annotation.StringRes;

public class ConfirmActionDialog extends BaseDialog {

    private static final String ARG_TITLE_STRING_ID = "titleStringId";
    private static final String ARG_MESSAGE_STRING_ID = "messageStringId";
    private static final String ARG_DIALOG_ID = "dialogId";

    private OnProceedListener _onProceedListener;

    private CheckBox _dontAskAgainCheckBox;

    public static ConfirmActionDialog newInstance(@StringRes int titleStringId,
                                                  @StringRes int messageStringId,
                                                  String dialogId) {
        ConfirmActionDialog result = new ConfirmActionDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE_STRING_ID, titleStringId);
        args.putInt(ARG_MESSAGE_STRING_ID, messageStringId);
        args.putString(ARG_DIALOG_ID, dialogId);
        result.setArguments(args);
        return result;
    }

    public void setOnProceedListener(OnProceedListener listener) {
        _onProceedListener = listener;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        if (getArguments() != null) {

            TextView titleTextView = view.findViewById(R.id.text_view_dialog_title);
            titleTextView.setText(getArguments().getInt(ARG_TITLE_STRING_ID));

            TextView messageTextView = view.findViewById(R.id.text_view_dialog_message);
            messageTextView.setText(getArguments().getInt(ARG_MESSAGE_STRING_ID));

        }

        _dontAskAgainCheckBox = view.findViewById(R.id.check_box_dont_ask_again);
    }

    @Override
    protected void okPressed() {

        // If the user checked the "don't ask me again" checkbox, set a preference.
        if (_dontAskAgainCheckBox.isChecked()) {
            _prefsUtil.setDontAskAgain(getArguments().getString(ARG_DIALOG_ID), true);
        }

        // Dismiss the dialog.
        dismiss();

        // Perform callback.
        if (_onProceedListener != null) {
            _onProceedListener.proceed();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_confirm_action;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }

    public interface OnProceedListener {
        void proceed();
    }
}
