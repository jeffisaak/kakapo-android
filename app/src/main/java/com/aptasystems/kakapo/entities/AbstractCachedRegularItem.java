package com.aptasystems.kakapo.entities;

import com.aptasystems.kakapo.adapter.model.NewsListItemState;

import io.requery.Entity;
import io.requery.Key;
import io.requery.Table;

@Entity
@Table(name = "cached_regular_item")
public class AbstractCachedRegularItem {

    @Key
    long _remoteId;

    String _ownerGuid;

    long _itemTimestamp;

    NewsListItemState _state;

    byte[] _thumbnailData;

    String _title;

    String _url;

    String _message;

    public void setRemoteId(long remoteId) {
        _remoteId = remoteId;
    }

}
