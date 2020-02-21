package com.aptasystems.kakapo.event;

public class FetchItemHeadersRequested {

    private long _lowItemRemoteId;

    public FetchItemHeadersRequested(long lowItemRemoteId) {
        _lowItemRemoteId = lowItemRemoteId;
    }

    public long getLowItemRemoteId() {
        return _lowItemRemoteId;
    }
}
