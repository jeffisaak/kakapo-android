package com.aptasystems.kakapo.service;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class AccountBackupInfo implements Serializable {

    private static final String PREFIX = "Kakapo";
    private static final String DELIMITER = ";";

    private String _guid;
    private String _apiKey;
    private String _passwordSalt;

    public static AccountBackupInfo from(String encoded) {
        String[] tokens = encoded.split(DELIMITER);
        if (tokens.length == 4) {
            return new AccountBackupInfo(tokens[1], tokens[2], tokens[3]);
        }
        return null;
    }

    public AccountBackupInfo(String guid, String apiKey, String passwordSalt) {
        _guid = guid;
        _apiKey = apiKey;
        _passwordSalt = passwordSalt;
    }

    public String getGuid() {
        return _guid;
    }

    public String getApiKey() {
        return _apiKey;
    }

    public String getPasswordSalt() {
        return _passwordSalt;
    }

    @NonNull
    @Override
    public String toString() {
        return new StringBuilder().append(PREFIX)
                .append(DELIMITER)
                .append(_guid)
                .append(DELIMITER)
                .append(_apiKey)
                .append(DELIMITER)
                .append(_passwordSalt)
                .toString();
    }
}
