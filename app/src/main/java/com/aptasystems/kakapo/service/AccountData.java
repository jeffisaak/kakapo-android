package com.aptasystems.kakapo.service;

import java.util.List;

/**
 * POJO to contain deserialized account data.
 */
public class AccountData {

    private long _timestampInGmt;
    private long _remoteBackupVersionNumber;
    private UserAccount _userAccount;
    private List<PreKey> _preKeys;
    private List<Group> _groups;
    private List<Friend> _friends;
    private List<String> _ignoredUserGuids;
    private List<Long> _ignoredItemRemoteIds;

    public long getTimestampInGmt() {
        return _timestampInGmt;
    }

    public void setTimestampInGmt(long timestampInGmt) {
        _timestampInGmt = timestampInGmt;
    }

    public long getRemoteBackupVersionNumber() {
        return _remoteBackupVersionNumber;
    }

    public void setRemoteBackupVersionNumber(long remoteBackupVersionNumber) {
        _remoteBackupVersionNumber = remoteBackupVersionNumber;
    }

    public UserAccount getUserAccount() {
        return _userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        _userAccount = userAccount;
    }

    public List<PreKey> getPreKeys() {
        return _preKeys;
    }

    public void setPreKeys(List<PreKey> preKeys) {
        _preKeys = preKeys;
    }

    public List<Friend> getFriends() {
        return _friends;
    }

    public void setFriends(List<Friend> friends) {
        _friends = friends;
    }

    public List<Group> getGroups() {
        return _groups;
    }

    public void setGroups(List<Group> groups) {
        _groups = groups;
    }

    public List<String> getIgnoredUserGuids() {
        return _ignoredUserGuids;
    }

    public void setIgnoredUserGuids(List<String> ignoredUserGuids) {
        _ignoredUserGuids = ignoredUserGuids;
    }

    public List<Long> getIgnoredItemRemoteIds() {
        return _ignoredItemRemoteIds;
    }

    public void setIgnoredItemRemoteIds(List<Long> ignoredItemRemoteIds) {
        _ignoredItemRemoteIds = ignoredItemRemoteIds;
    }

    public static class UserAccount {
        private String _guid;
        private String _name;
        private String _passwordSalt;
        private String _signingPublicKey;
        private String _encryptedSigningSecretKey;
        private String _signingSecretKeyNonce;
        private String _apiKey;
        private Integer _colour;
        private Long _remoteBackupVersionNumber;

        public String getGuid() {
            return _guid;
        }

        public void setGuid(String guid) {
            _guid = guid;
        }

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }

        public String getPasswordSalt() {
            return _passwordSalt;
        }

        public void setPasswordSalt(String passwordSalt) {
            _passwordSalt = passwordSalt;
        }

        public String getSigningPublicKey() {
            return _signingPublicKey;
        }

        public void setSigningPublicKey(String signingPublicKey) {
            _signingPublicKey = signingPublicKey;
        }

        public String getEncryptedSigningSecretKey() {
            return _encryptedSigningSecretKey;
        }

        public void setEncryptedSigningSecretKey(String encryptedSigningSecretKey) {
            _encryptedSigningSecretKey = encryptedSigningSecretKey;
        }

        public String getSigningSecretKeyNonce() {
            return _signingSecretKeyNonce;
        }

        public void setSigningSecretKeyNonce(String signingSecretKeyNonce) {
            _signingSecretKeyNonce = signingSecretKeyNonce;
        }

        public String getApiKey() {
            return _apiKey;
        }

        public void setApiKey(String apiKey) {
            _apiKey = apiKey;
        }

        public Integer getColour() {
            return _colour;
        }

        public void setColour(Integer colour) {
            _colour = colour;
        }

        public Long getRemoteBackupVersionNumber() {
            return _remoteBackupVersionNumber;
        }

        public void setRemoteBackupVersionNumber(Long remoteBackupVersionNumber) {
            _remoteBackupVersionNumber = remoteBackupVersionNumber;
        }
    }

    public static class PreKey {
        private Long _preKeyId;
        private String _publicKey;
        private String _secretKey;

        public Long getPreKeyId() {
            return _preKeyId;
        }

        public void setPreKeyId(Long preKeyId) {
            _preKeyId = preKeyId;
        }

        public String getPublicKey() {
            return _publicKey;
        }

        public void setPublicKey(String publicKey) {
            _publicKey = publicKey;
        }

        public String getSecretKey() {
            return _secretKey;
        }

        public void setSecretKey(String secretKey) {
            _secretKey = secretKey;
        }
    }

    public static class Group {
        private String _name;
        private List<String> _memberGuids;

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }

        public List<String> getMemberGuids() {
            return _memberGuids;
        }

        public void setMemberGuids(List<String> memberGuids) {
            _memberGuids = memberGuids;
        }
    }

    public static class Friend {
        private String _guid;
        private String _name;
        private String _signingPublicKey;
        private int _colour;

        public String getGuid() {
            return _guid;
        }

        public void setGuid(String guid) {
            _guid = guid;
        }

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }

        public String getSigningPublicKey() {
            return _signingPublicKey;
        }

        public void setSigningPublicKey(String signingPublicKey) {
            _signingPublicKey = signingPublicKey;
        }

        public int getColour() {
            return _colour;
        }

        public void setColour(int colour) {
            _colour = colour;
        }
    }
}
