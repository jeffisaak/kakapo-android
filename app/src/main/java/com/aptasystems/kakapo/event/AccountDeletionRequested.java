package com.aptasystems.kakapo.event;

public class AccountDeletionRequested {

    private Long _userAccountId;
    private boolean _deleteFromServer;

    public AccountDeletionRequested(Long userAccountId, boolean deleteFromServer) {
        _userAccountId = userAccountId;
        _deleteFromServer = deleteFromServer;
    }

    public Long getUserAccountId() {
        return _userAccountId;
    }

    public boolean isDeleteFromServer() {
        return _deleteFromServer;
    }
}
