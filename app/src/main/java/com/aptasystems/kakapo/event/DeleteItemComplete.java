package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class DeleteItemComplete {

    private AsyncResult _status;
    private long _itemRemoteId;
    private int _usedQuota;
    private int _maxQuota;

    public static DeleteItemComplete success(long itemRemoteId, int usedQuota, int maxQuota) {
        DeleteItemComplete result = new DeleteItemComplete();
        result.setStatus(AsyncResult.Success);
        result.setItemRemoteId(itemRemoteId);
        result.setUsedQuota(usedQuota);
        result.setMaxQuota(maxQuota);
        return result;
    }
    public static DeleteItemComplete failure(AsyncResult status) {
        DeleteItemComplete result = new DeleteItemComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public long getItemRemoteId() {
        return _itemRemoteId;
    }

    public void setItemRemoteId(long itemRemoteId) {
        _itemRemoteId = itemRemoteId;
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
