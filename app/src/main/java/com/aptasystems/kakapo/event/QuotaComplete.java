package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class QuotaComplete {

    private AsyncResult _status;
    private int _usedQuota;
    private int _maxQuota;

    public static QuotaComplete success(int usedQuota, int maxQuota) {
        QuotaComplete result = new QuotaComplete();
        result.setStatus(AsyncResult.Success);
        result.setUsedQuota(usedQuota);
        result.setMaxQuota(maxQuota);
        return result;
    }

    public static QuotaComplete failure(AsyncResult status) {
        QuotaComplete result = new QuotaComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public int getUsedQuota() {
        return _usedQuota;
    }

    public void setUsedQuota(int usedQuota) {
        _usedQuota = usedQuota;
    }

    public int getMaxQuota() {
        return _maxQuota;
    }

    public void setMaxQuota(int maxQuota) {
        _maxQuota = maxQuota;
    }
}
