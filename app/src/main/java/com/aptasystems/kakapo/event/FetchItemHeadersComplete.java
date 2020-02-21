package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

import java.util.List;

import kakapo.api.model.ShareItem;

public class FetchItemHeadersComplete {

    private AsyncResult _status;
    private Class<?> _eventTarget;
    private List<ShareItem> _shareItems;
    private Long _remainingItemCount;

    public static FetchItemHeadersComplete success(Class<?> eventTarget, List<ShareItem> shareItems, Long remainingItemCount) {
        FetchItemHeadersComplete result = new FetchItemHeadersComplete();
        result.setStatus(AsyncResult.Success);
        result.setEventTarget(eventTarget);
        result.setShareItems(shareItems);
        result.setRemainingItemCount(remainingItemCount);
        return result;
    }

    public static FetchItemHeadersComplete failure(Class<?> eventTarget, AsyncResult status) {
        FetchItemHeadersComplete result = new FetchItemHeadersComplete();
        result.setEventTarget(eventTarget);
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public Class<?> getEventTarget() {
        return _eventTarget;
    }

    public void setEventTarget(Class<?> eventTarget) {
        _eventTarget = eventTarget;
    }

    public List<ShareItem> getShareItems() {
        return _shareItems;
    }

    public void setShareItems(List<ShareItem> shareItems) {
        _shareItems = shareItems;
    }

    public Long getRemainingItemCount() {
        return _remainingItemCount;
    }

    public void setRemainingItemCount(Long remainingItemCount) {
        _remainingItemCount = remainingItemCount;
    }
}
