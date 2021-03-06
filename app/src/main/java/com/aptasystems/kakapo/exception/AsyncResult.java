package com.aptasystems.kakapo.exception;

public enum AsyncResult {
    Success,
    OtherHttpError,
    ServerUnavailable,
    RetrofitIOException,
    Unauthorized,
    NotFound,
    PayloadTooLarge,
    TooManyRequests,
    KeyGenerationFailed,
    KeyVerificationFailed,
    KeySerializationFailed,
    IncorrectPassword,
    ItemSerializationFailed,
    AccountEncryptionFailed,
    KeyEncryptionFailed,
    ShareEncryptionFailed,
    AccountSerializationFailed,
    DecryptionFailed,
    AccountDeserializationFailed,
    ItemDeserializationFailed,
    ContentStreamFailed,
    QuotaExceeded,
    InvalidKeyLength,
    NoPreKeysAvailable,
    FetchPreKeyError,
    BadRequest,
    Conflict,
    PreKeyNotFound
}
