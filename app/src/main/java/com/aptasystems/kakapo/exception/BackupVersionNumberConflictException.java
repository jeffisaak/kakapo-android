package com.aptasystems.kakapo.exception;

public class BackupVersionNumberConflictException extends ApiException {
    public BackupVersionNumberConflictException() {
        super(AsyncResult.Conflict);
    }
}
