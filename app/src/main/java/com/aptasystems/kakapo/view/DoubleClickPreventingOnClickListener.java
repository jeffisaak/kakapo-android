package com.aptasystems.kakapo.view;

import android.os.SystemClock;
import android.view.View;

public abstract class DoubleClickPreventingOnClickListener implements View.OnClickListener {

    private long _lastClickTime = 0L;

    @Override
    public final void onClick(View v) {
        if (SystemClock.elapsedRealtime() - _lastClickTime < 300) {
            return;
        }
        _lastClickTime = SystemClock.elapsedRealtime();

        onClickInternal(v);
    }

    protected abstract void onClickInternal(View v);
}
