package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class DownloadAccountComplete {

    private AsyncResult _status;

    public static DownloadAccountComplete success() {
        DownloadAccountComplete result = new DownloadAccountComplete();
        result.setStatus(AsyncResult.Success);
        return result;
    }

    public static DownloadAccountComplete failure(AsyncResult status) {
        DownloadAccountComplete result = new DownloadAccountComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }
}
