package com.aptasystems.kakapo.service;

class EncryptedDataWithMetadata {

    private byte[] _encryptedData;
    private Long _preKeyId;
    private String _keyExchangePublicKey;
    private String _encryptedGroupKey;
    private String _nonce;
    private String _contentNonce;

    EncryptedDataWithMetadata(byte[] encryptedData, Long preKeyId, String keyExchangePublicKey, String encryptedGroupKey, String nonce, String contentNonce) {
        _encryptedData = encryptedData;
        _preKeyId = preKeyId;
        _keyExchangePublicKey = keyExchangePublicKey;
        _encryptedGroupKey = encryptedGroupKey;
        _nonce = nonce;
        _contentNonce = contentNonce;
    }

    byte[] getEncryptedData() {
        return _encryptedData;
    }

    Long getPreKeyId() {
        return _preKeyId;
    }

    String getKeyExchangePublicKey() {
        return _keyExchangePublicKey;
    }

    String getEncryptedGroupKey() {
        return _encryptedGroupKey;
    }

    String getNonce() {
        return _nonce;
    }

    String getContentNonce() {
        return _contentNonce;
    }
}
