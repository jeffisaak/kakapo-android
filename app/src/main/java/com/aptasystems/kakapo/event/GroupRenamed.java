package com.aptasystems.kakapo.event;

public class GroupRenamed {

    private Long _groupId;
    private String _name;

    public GroupRenamed(Long groupId, String name) {
        _groupId = groupId;
        _name = name;
    }

    public Long getGroupId() {
        return _groupId;
    }

    public String getName() {
        return _name;
    }
}

