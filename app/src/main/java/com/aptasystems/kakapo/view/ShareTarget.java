package com.aptasystems.kakapo.view;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class ShareTarget implements Serializable {

    private String _name;
    private String _guid;
    private Long _groupId;

    public ShareTarget(String name, String guid, Long groupId) {
        _name = name;
        _guid = guid;
        _groupId = groupId;
    }

    public String getName() {
        return _name;
    }

    public String getGuid() {
        return _guid;
    }

    public Long getGroupId() {
        return _groupId;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(_name);
        if (_guid != null) {
            stringBuilder.append(" (").append(_guid).append(")");
        }
        return stringBuilder.toString();
    }
}
