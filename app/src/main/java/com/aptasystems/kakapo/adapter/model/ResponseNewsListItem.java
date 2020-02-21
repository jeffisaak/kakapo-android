package com.aptasystems.kakapo.adapter.model;

import java.io.Serializable;

public class ResponseNewsListItem extends AbstractNewsListItem implements Serializable {

    private String _message;

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        _message = message;
    }

}
