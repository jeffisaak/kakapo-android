package com.aptasystems.kakapo.event;

import com.aptasystems.kakapo.exception.AsyncResult;

public class ContentStreamComplete {

    private AsyncResult _status;
    private Long _itemRemoteId;
    private byte[] _encryptedContent;
    private Long _preKeyId;
    private String _keyExchangePublicKey;
    private String _encryptedGroupKey;
    private String _nonce;
    private String _contentNonce;

    public static ContentStreamComplete success(long itemRemoteId,
                                                byte[] encryptedContent,
                                                Long preKeyId,
                                                String keyExchangePublicKey,
                                                String encryptedGroupKey,
                                                String nonce,
                                                String contentNonce) {
        ContentStreamComplete result = new ContentStreamComplete();
        result.setStatus(AsyncResult.Success);
        result.setItemRemoteId(itemRemoteId);
        result.setEncryptedContent(encryptedContent);
        result.setPreKeyId(preKeyId);
        result.setKeyExchangePublicKey(keyExchangePublicKey);
        result.setEncryptedGroupKey(encryptedGroupKey);
        result.setNonce(nonce);
        result.setContentNonce(contentNonce);
        return result;
    }

    public static ContentStreamComplete failure(AsyncResult status) {
        ContentStreamComplete result = new ContentStreamComplete();
        result.setStatus(status);
        return result;
    }

    public AsyncResult getStatus() {
        return _status;
    }

    public void setStatus(AsyncResult status) {
        _status = status;
    }

    public Long getItemRemoteId() {
        return _itemRemoteId;
    }

    public void setItemRemoteId(Long itemRemoteId) {
        _itemRemoteId = itemRemoteId;
    }

    public byte[] getEncryptedContent() {
        return _encryptedContent;
    }

    public void setEncryptedContent(byte[] encryptedContent) {
        _encryptedContent = encryptedContent;
    }

    public Long getPreKeyId() {
        return _preKeyId;
    }

    public void setPreKeyId(Long preKeyId) {
        _preKeyId = preKeyId;
    }

    public String getKeyExchangePublicKey() {
        return _keyExchangePublicKey;
    }

    public void setKeyExchangePublicKey(String keyExchangePublicKey) {
        _keyExchangePublicKey = keyExchangePublicKey;
    }

    public String getEncryptedGroupKey() {
        return _encryptedGroupKey;
    }

    public void setEncryptedGroupKey(String encryptedGroupKey) {
        _encryptedGroupKey = encryptedGroupKey;
    }

    public String getNonce() {
        return _nonce;
    }

    public void setNonce(String nonce) {
        _nonce = nonce;
    }

    public String getContentNonce() {
        return _contentNonce;
    }

    public void setContentNonce(String contentNonce) {
        _contentNonce = contentNonce;
    }
}
