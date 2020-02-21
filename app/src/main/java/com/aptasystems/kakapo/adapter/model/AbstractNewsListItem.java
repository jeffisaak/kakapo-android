package com.aptasystems.kakapo.adapter.model;

import java.io.Serializable;

public abstract class AbstractNewsListItem implements Serializable {

    private Long _localId;
    private Long _remoteId;
    private String _ownerGuid;
    private Long _parentItemRemoteId;
    private long _itemTimestamp;
    private NewsListItemState _state;

    public Long getLocalId() {
        return _localId;
    }

    public void setLocalId(Long localId) {
        _localId = localId;
    }

    public Long getRemoteId() {
        return _remoteId;
    }

    public void setRemoteId(Long remoteId) {
        _remoteId = remoteId;
    }

    public String getOwnerGuid() {
        return _ownerGuid;
    }

    public void setOwnerGuid(String ownerGuid) {
        _ownerGuid = ownerGuid;
    }

    public Long getParentItemRemoteId() {
        return _parentItemRemoteId;
    }

    public void setParentItemRemoteId(Long parentItemRemoteId) {
        _parentItemRemoteId = parentItemRemoteId;
    }

    public long getItemTimestamp() {
        return _itemTimestamp;
    }

    public void setItemTimestamp(long itemTimestamp) {
        _itemTimestamp = itemTimestamp;
    }

    public NewsListItemState getState() {
        return _state;
    }

    public void setState(NewsListItemState state) {
        _state = state;
    }

    public boolean isOwnedBy(String guid) {
        return guid.compareTo(_ownerGuid) == 0;
    }
}
