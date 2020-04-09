package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.aptasystems.kakapo.R;

import androidx.annotation.StringRes;

public class AnnouncementDialog extends BaseDialog {

    private static final String ARG_TITLE_STRING_ID = "titleStringId";
    private static final String ARG_MESSAGE_STRING_ID = "messageStringId";

    public static AnnouncementDialog newInstance(@StringRes int titleStringId,
                                                 @StringRes int messageStringId) {
        AnnouncementDialog result = new AnnouncementDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE_STRING_ID, titleStringId);
        args.putInt(ARG_MESSAGE_STRING_ID, messageStringId);
        result.setArguments(args);
        return result;
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
    }

    @Override
    protected void okPressed() {
        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_announcement;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
