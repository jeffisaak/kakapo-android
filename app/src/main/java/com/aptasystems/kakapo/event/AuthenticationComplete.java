package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class AuthenticationComplete {

    private AsyncResult _status;
    private Long _userAccountId;
    private String _hashedPassword;

    public static AuthenticationComplete success(long userAccountId, String hashedPassword) {
        AuthenticationComplete result = new AuthenticationComplete();
        result.setStatus(AsyncResult.Success);
        result.setUserAccountId(userAccountId);
        result.setHashedPassword(hashedPassword);
        return result;
    }

    public static AuthenticationComplete failure(AsyncResult status) {
        AuthenticationComplete result = new AuthenticationComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public Long getUserAccountId() {
        return _userAccountId;
    }

    public void setUserAccountId(Long userAccountId) {
        _userAccountId = userAccountId;
    }

    public String getHashedPassword() {
        return _hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        _hashedPassword = hashedPassword;
    }
}
