package com.aptasystems.kakapo.event;

public class SubmitItemStarted {

    private long _shareItemId;

    public SubmitItemStarted(long shareItemId) {
        _shareItemId = shareItemId;
    }

    public long getShareItemId() {
        return _shareItemId;
    }
}

