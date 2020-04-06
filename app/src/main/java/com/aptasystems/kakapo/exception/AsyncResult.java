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
    KeySerializationFailed,
    IncorrectPassword,
    ItemSerializationFailed,
    EncryptionFailed,
    AccountSerializationFailed,
    DecryptionFailed,
    AccountDeserializationFailed,
    ItemDeserializationFailed,
    ContentStreamFailed,
    QuotaExceeded,
    InsufficientKeyLength,
    NoPreKeysAvailable,
    BadRequest
}
