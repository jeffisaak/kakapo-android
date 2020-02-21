package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class SubmitItemComplete {

    private Class<?> _eventTarget;
    private AsyncResult _status;
    private long _itemLocalId;
    private long _itemRemoteId;
    private int _usedQuota;
    private int _maxQuota;

    public static SubmitItemComplete success(Class<?> eventTarget,
                                             long itemRemoteId,
                                             int usedQuota,
                                             int maxQuota) {
        SubmitItemComplete result = new SubmitItemComplete();
        result.setEventTarget(eventTarget);
        result.setStatus(AsyncResult.Success);
        result.setItemRemoteId(itemRemoteId);
        result.setUsedQuota(usedQuota);
        result.setMaxQuota(maxQuota);
        return result;
    }

    public static SubmitItemComplete failure(Class<?> eventTarget, AsyncResult status, long itemId) {
        SubmitItemComplete result = new SubmitItemComplete();
        result.setEventTarget(eventTarget);
        result.setStatus(status);
        result.setItemLocalId(itemId);
        return result;
    }

    public Class<?> getEventTarget() {
        return _eventTarget;
    }

    public void setEventTarget(Class<?> eventTarget) {
        _eventTarget = eventTarget;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public long getItemLocalId() {
        return _itemLocalId;
    }

    public void setItemLocalId(long itemLocalId) {
        _itemLocalId = itemLocalId;
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
