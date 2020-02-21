package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.exception.AsyncResult;

public class NewsItemDecryptComplete {

    private Class<?> _eventTarget;
    private AsyncResult _status;
    private long _newsItemRemoteId;
    private AbstractNewsListItem _newsItem;

    public static NewsItemDecryptComplete success(Class<?> eventTarget, AbstractNewsListItem newsItem) {
        NewsItemDecryptComplete result = new NewsItemDecryptComplete();
        result.setEventTarget(eventTarget);
        result.setStatus(AsyncResult.Success);
        result.setNewsItem(newsItem);
        return result;
    }

    public static NewsItemDecryptComplete failure(Class<?> eventTarget, long newsItemRemoteId, AsyncResult status) {
        NewsItemDecryptComplete result = new NewsItemDecryptComplete();
        result.setEventTarget(eventTarget);
        result.setNewsItemRemoteId(newsItemRemoteId);
        result.setStatus(status);
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

    public long getNewsItemRemoteId() {
        return _newsItemRemoteId;
    }

    public void setNewsItemRemoteId(long newsItemRemoteId) {
        _newsItemRemoteId = newsItemRemoteId;
    }

    public AbstractNewsListItem getNewsItem() {
        return _newsItem;
    }

    public void setNewsItem(AbstractNewsListItem newsItem) {
        _newsItem = newsItem;
    }
}
