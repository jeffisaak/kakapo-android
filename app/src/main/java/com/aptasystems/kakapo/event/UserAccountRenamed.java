package com.aptasystems.kakapo.event;

public class UserAccountRenamed {

    private String _name;

    public UserAccountRenamed(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }
}
