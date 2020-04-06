package com.aptasystems.kakapo.event;

public class UploadPreKeysRequested {

    private long _userAccountId;
    private String _password;

    public UploadPreKeysRequested(long userAccountId, String password) {
        _userAccountId = userAccountId;
        _password = password;
    }

    public long getUserAccountId() {
        return _userAccountId;
    }

    public String getPassword() {
        return _password;
    }
}
