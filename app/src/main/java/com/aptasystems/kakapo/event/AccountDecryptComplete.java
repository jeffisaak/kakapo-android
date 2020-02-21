package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.service.AccountData;

public class AccountDecryptComplete {

    private AccountData _accountData;
    private boolean _serializationError;
    private boolean _decryptionError;

    public static AccountDecryptComplete success(AccountData accountData) {
        AccountDecryptComplete result = new AccountDecryptComplete();
        result.setAccountData(accountData);
        result.setSerializationError(false);
        result.setDecryptionError(false);
        return result;
    }

    public static AccountDecryptComplete serializationError() {
        AccountDecryptComplete result = new AccountDecryptComplete();
        result.setSerializationError(true);
        result.setDecryptionError(false);
        return result;
    }

    public static AccountDecryptComplete decryptionError() {
        AccountDecryptComplete result = new AccountDecryptComplete();
        result.setSerializationError(false);
        result.setDecryptionError(true);
        return result;
    }

    public AccountData getAccountData() {
        return _accountData;
    }

    public void setAccountData(AccountData accountData) {
        _accountData = accountData;
    }

    public boolean isSerializationError() {
        return _serializationError;
    }

    public void setSerializationError(boolean serializationError) {
        _serializationError = serializationError;
    }

    public boolean isDecryptionError() {
        return _decryptionError;
    }

    public void setDecryptionError(boolean decryptionError) {
        _decryptionError = decryptionError;
    }
}
