package com.aptasystems.kakapo.adapter.model;

import java.io.Serializable;

public class RegularNewsListItem extends AbstractNewsListItem implements Serializable {

    private String _title;
    private String _url;
    private byte[] _thumbnailData;
    private String _message;

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        _url = url;
    }

    public byte[] getThumbnailData() {
        return _thumbnailData;
    }

    public void setThumbnailData(byte[] thumbnailData) {
        _thumbnailData = thumbnailData;
    }

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        _message = message;
    }

}
