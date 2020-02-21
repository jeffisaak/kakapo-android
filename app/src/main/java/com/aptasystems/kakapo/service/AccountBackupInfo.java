package com.aptasystems.kakapo.service;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class AccountBackupInfo implements Serializable {

        private static final String DELIMITER = ";";

        private String _backupGuid;
        private String _password;
        private String _salt;

        public static AccountBackupInfo from(String encoded) {
            String[] tokens = encoded.split(DELIMITER);
            return new AccountBackupInfo(tokens[0], tokens[1], tokens[2]);
        }

        public AccountBackupInfo(String backupGuid, String password, String salt) {
            _backupGuid = backupGuid;
            _password = password;
            _salt = salt;
        }

        public String getBackupGuid() {
            return _backupGuid;
        }

        public String getPassword() {
            return _password;
        }

        public String getSalt() {
            return _salt;
        }

        @NonNull
        @Override
        public String toString() {
            return new StringBuilder()
                    .append(_backupGuid)
                    .append(DELIMITER)
                    .append(_password)
                    .append(DELIMITER)
                    .append(_salt)
                    .toString();
        }
    }
