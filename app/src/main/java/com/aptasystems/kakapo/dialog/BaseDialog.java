package com.aptasystems.kakapo.dialog;

import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.GroupDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.service.AccountBackupService;
import com.aptasystems.kakapo.service.AccountRestoreService;
import com.aptasystems.kakapo.service.FriendService;
import com.aptasystems.kakapo.service.UserAccountService;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public abstract class BaseDialog extends DialogFragment {

    protected static final String ARG_USER_ACCOUNT_ID = "userAccountId";

    @Inject
    UserAccountService _userAccountService;

    @Inject
    FriendService _friendService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    EventBus _eventBus;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    FriendDAO _friendDAO;

    @Inject
    GroupDAO _groupDAO;

    @Inject
    AccountBackupService _accountBackupService;

    @Inject
    AccountRestoreService _accountRestoreService;

    @Override
    public final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((KakapoApplication) getActivity().getApplication()).getKakapoComponent().inject(this);
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
                                   @Nullable ViewGroup container,
                                   @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(getLayoutId(), container, false);
        onCreateViewInternal(view);

        // Set the on click listeners for the ok and cancel buttons.
        Button okButton = view.findViewById(R.id.button_ok);
        if (okButton != null) {
            okButton.setOnClickListener(v -> okPressed());
        }
        if (view.findViewById(R.id.button_cancel) != null) {
            view.findViewById(R.id.button_cancel).setOnClickListener(v -> {
                cancelPressed();
                dismiss();
            });
        }

        return view;
    }

    @Override
    public final void onCancel(@NonNull DialogInterface dialog) {
        cancelPressed();
    }

    protected abstract void onCreateViewInternal(View view);

    protected abstract void okPressed();

    protected void cancelPressed() {
        // Let subclasses override.
    }

    protected abstract int getLayoutId();

    protected abstract boolean usesEventBus();

    @Override
    public final void onResume() {
        super.onResume();

        // Register to listen for events.
        if (!_eventBus.isRegistered(this) && usesEventBus()) {
            _eventBus.register(this);
        }

        // Min height and max width calculations
        WindowManager wm = getDialog().getWindow().getWindowManager();
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int windowWidth = size.x;

        final int windowHorizontalPadding = getContext().getResources().getDimensionPixelSize(R.dimen.dialog_horizontal_margin);
        final int maxWidth = getContext().getResources().getDimensionPixelSize(R.dimen.dialog_max_width);
        final int calculatedWidth = windowWidth - (windowHorizontalPadding * 2);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getDialog().getWindow().getAttributes());
        lp.width = Math.min(maxWidth, calculatedWidth);
        getDialog().getWindow().setAttributes(lp);
    }

    @Override
    public final void onPause() {
        super.onPause();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

}
