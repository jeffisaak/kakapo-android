package com.aptasystems.kakapo.event;

public class UserAccountSelected {

    private Long _userAccountId;

    public UserAccountSelected(Long userAccountId) {
        _userAccountId = userAccountId;
    }

    public Long getUserAccountId() {
        return _userAccountId;
    }
}
