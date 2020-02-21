package com.aptasystems.kakapo.event;

import java.io.File;

public class AccountBackupComplete {

    private File _encryptedBackupFile;
    private boolean _serializationError;
    private boolean _encryptionError;

    public static AccountBackupComplete success(File encryptedBackupFile) {
        AccountBackupComplete result = new AccountBackupComplete();
        result.setEncryptedBackupFile(encryptedBackupFile);
        result.setSerializationError(false);
        result.setEncryptionError(false);
        return result;
    }

    public static AccountBackupComplete serializationError() {
        AccountBackupComplete result = new AccountBackupComplete();
        result.setSerializationError(true);
        result.setEncryptionError(false);
        return result;
    }

    public static AccountBackupComplete encryptionError() {
        AccountBackupComplete result = new AccountBackupComplete();
        result.setSerializationError(false);
        result.setEncryptionError(true);
        return result;
    }

    public File getEncryptedBackupFile() {
        return _encryptedBackupFile;
    }

    public void setEncryptedBackupFile(File encryptedBackupFile) {
        _encryptedBackupFile = encryptedBackupFile;
    }

    public boolean isSerializationError() {
        return _serializationError;
    }

    public void setSerializationError(boolean serializationError) {
        _serializationError = serializationError;
    }

    public boolean isEncryptionError() {
        return _encryptionError;
    }

    public void setEncryptionError(boolean encryptionError) {
        _encryptionError = encryptionError;
    }
}
