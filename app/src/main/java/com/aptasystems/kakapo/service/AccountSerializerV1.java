package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.entities.IgnoredItem;
import com.aptasystems.kakapo.entities.IgnoredPerson;
import com.aptasystems.kakapo.entities.PreKey;
import com.aptasystems.kakapo.entities.UserAccount;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import kakapo.util.SerializationUtil;
import kakapo.util.TimeUtil;

public class AccountSerializerV1 implements IAccountSerializer {

    private static final int VERSION_NUMBER = 1;

    private EntityDataStore<Persistable> _entityStore;

    public AccountSerializerV1(EntityDataStore<Persistable> entityStore) {
        _entityStore = entityStore;
    }

    @Override
    public byte[] serializeUserAccountData(long userAccountId) throws IOException {

        // Fetch data from the data store.
        UserAccount userAccount = _entityStore.select(UserAccount.class)
                .where(UserAccount.ID.eq(userAccountId))
                .get()
                .firstOrNull();
        Result<Friend> friends = _entityStore.select(Friend.class)
                .where(Friend.USER_ACCOUNT_ID.eq(userAccountId))
                .get();
        Result<Group> groups = _entityStore.select(Group.class)
                .where(Group.USER_ACCOUNT_ID.eq(userAccountId))
                .get();
        Result<PreKey> preKeys = _entityStore.select(PreKey.class)
                .where(PreKey.USER_ACCOUNT_ID.eq(userAccountId))
                .get();

        List<GroupMember> allGroupMembers = new ArrayList<>();
        for (Group group : userAccount.getGroups()) {
            Result<GroupMember> oneGroupMembers = _entityStore.select(GroupMember.class)
                    .where(GroupMember.GROUP_ID.eq(group.getId()))
                    .get();
            allGroupMembers.addAll(oneGroupMembers.toList());
        }
        Result<IgnoredPerson> ignoredPeople = _entityStore.select(IgnoredPerson.class)
                .where(IgnoredPerson.USER_ACCOUNT_ID.eq(userAccountId))
                .get();
        Result<IgnoredItem> ignoredItems = _entityStore.select(IgnoredItem.class)
                .where(IgnoredItem.USER_ACCOUNT_ID.eq(userAccountId))
                .get();

        // Open our streams.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        // Write the version number
        dataOutputStream.writeInt(VERSION_NUMBER);

        // Write timestamp.
        dataOutputStream.writeLong(TimeUtil.timestampInGMT());

        // Write user account stuff.
        serializeUserAccount(dataOutputStream, userAccount);
        serializePreKeys(dataOutputStream, preKeys.toList());
        serializeGroups(dataOutputStream, groups.toList(), allGroupMembers);
        serializeFriends(dataOutputStream, friends.toList());
        serializeIgnoredPeople(dataOutputStream, ignoredPeople.toList());
        serializeIgnoredItems(dataOutputStream, ignoredItems.toList());

        // Close streams.
        dataOutputStream.flush();
        dataOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    private void serializeUserAccount(DataOutputStream outputStream, UserAccount userAccount)
            throws IOException {
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getGuid());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getName());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getPasswordSalt());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getSigningPublicKey());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getEncryptedSigningSecretKey());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getSigningSecretKeyNonce());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getApiKey());
        outputStream.writeInt(userAccount.getColour());
    }

    private void serializePreKeys(DataOutputStream outputStream, Collection<PreKey> preKeys) throws IOException {
        outputStream.writeInt(preKeys.size());
        for (PreKey preKey : preKeys) {
            outputStream.writeLong(preKey.getPreKeyId());
            SerializationUtil.writeOptionalUTF(outputStream, preKey.getPublicKey());
            SerializationUtil.writeOptionalUTF(outputStream, preKey.getSecretKey());
        }
    }

    private void serializeGroups(DataOutputStream outputStream,
                                 Collection<Group> groups,
                                 Collection<GroupMember> groupMembers)
            throws IOException {
        outputStream.writeInt(groups.size());
        for (Group group : groups) {
            SerializationUtil.writeOptionalUTF(outputStream, group.getName());

            // Build a list of member guids.
            List<String> memberGuids = new ArrayList<>();
            for (GroupMember groupMember : groupMembers) {
                if (groupMember.getGroup().getId() == group.getId()) {
                    memberGuids.add(groupMember.getFriend().getGuid());
                }
            }

            // Write out the count of members and then the guids.
            outputStream.writeInt(memberGuids.size());
            for (String memberGuid : memberGuids) {
                SerializationUtil.writeOptionalUTF(outputStream, memberGuid);
            }
        }
    }

    private void serializeFriends(DataOutputStream outputStream,
                                  Collection<Friend> friends) throws IOException {
        outputStream.writeInt(friends.size());
        for (Friend friend : friends) {
            SerializationUtil.writeOptionalUTF(outputStream, friend.getGuid());
            SerializationUtil.writeOptionalUTF(outputStream, friend.getName());
            SerializationUtil.writeOptionalUTF(outputStream, friend.getSigningPublicKey());
            outputStream.writeInt(friend.getColour());
        }
    }

    private void serializeIgnoredPeople(DataOutputStream outputStream,
                                        Collection<IgnoredPerson> ignoredPeople) throws IOException {
        outputStream.writeInt(ignoredPeople.size());
        for (IgnoredPerson ignoredPerson : ignoredPeople) {
            SerializationUtil.writeOptionalUTF(outputStream, ignoredPerson.getGuid());
        }
    }

    private void serializeIgnoredItems(DataOutputStream outputStream,
                                       Collection<IgnoredItem> ignoredItems) throws IOException {
        outputStream.writeInt(ignoredItems.size());
        for (IgnoredItem ignoredItem : ignoredItems) {
            outputStream.writeLong(ignoredItem.getItemRemoteId());
        }
    }

    @Override
    public AccountData deserializeUserAccountData(DataInputStream dataInputStream)
            throws IOException {

        // Our deserialized data will end up in here.
        AccountData accountData = new AccountData();

        // Read timestamp.
        accountData.setTimestampInGmt(dataInputStream.readLong());

        // Read account data.
        deserializeUserAccount(dataInputStream, accountData);
        deserializePreKeys(dataInputStream, accountData);
        deserializeGroups(dataInputStream, accountData);
        deserializeFriends(dataInputStream, accountData);
        deserializeIgnoredPeople(dataInputStream, accountData);
        deserializeIgnoredItems(dataInputStream, accountData);

        return accountData;
    }

    /**
     * Deserialize basic user account data from the specified input stream into the specified {@link
     * AccountData}.
     *
     * @param inputStream
     * @param accountData
     * @throws IOException
     */
    private void deserializeUserAccount(DataInputStream inputStream, AccountData accountData) throws IOException {

        AccountData.UserAccount userAccount = new AccountData.UserAccount();

        userAccount.setGuid(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setName(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setPasswordSalt(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setSigningPublicKey(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setEncryptedSigningSecretKey(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setSigningSecretKeyNonce(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setApiKey(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setColour(inputStream.readInt());

        accountData.setUserAccount(userAccount);
    }

    private void deserializePreKeys(DataInputStream inputStream, AccountData accountData) throws IOException {

        accountData.setPreKeys(new ArrayList<>());

        int preKeyCount = inputStream.readInt();
        for (int ii = 0; ii < preKeyCount; ii++) {
            AccountData.PreKey preKey = new AccountData.PreKey();
            preKey.setPreKeyId(inputStream.readLong());
            preKey.setPublicKey(SerializationUtil.readOptionalUTF(inputStream));
            preKey.setSecretKey(SerializationUtil.readOptionalUTF(inputStream));
            accountData.getPreKeys().add(preKey);
        }
    }

    /**
     * Deserialize the account's groups.
     *
     * @param inputStream
     * @param accountData
     * @throws IOException
     */
    private void deserializeGroups(DataInputStream inputStream, AccountData accountData) throws IOException {

        accountData.setGroups(new ArrayList<>());

        int groupCount = inputStream.readInt();
        for (int ii = 0; ii < groupCount; ii++) {
            AccountData.Group group = new AccountData.Group();
            group.setName(SerializationUtil.readOptionalUTF(inputStream));

            int memberCount = inputStream.readInt();
            List<String> memberGuids = new ArrayList<>();
            for (int jj = 0; jj < memberCount; jj++) {
                memberGuids.add(SerializationUtil.readOptionalUTF(inputStream));
            }
            group.setMemberGuids(memberGuids);
            accountData.getGroups().add(group);
        }
    }

    /**
     * Deserialize the account's friends.
     *
     * @param inputStream
     * @param accountData
     * @throws IOException
     */
    private void deserializeFriends(DataInputStream inputStream, AccountData accountData) throws IOException {

        accountData.setFriends(new ArrayList<>());

        int friendCount = inputStream.readInt();
        for (int ii = 0; ii < friendCount; ii++) {
            AccountData.Friend friend = new AccountData.Friend();
            friend.setGuid(SerializationUtil.readOptionalUTF(inputStream));
            friend.setName(SerializationUtil.readOptionalUTF(inputStream));
            friend.setSigningPublicKey(SerializationUtil.readOptionalUTF(inputStream));
            friend.setColour(inputStream.readInt());

            accountData.getFriends().add(friend);
        }
    }

    /**
     * Deserialize the account's ignored people.
     *
     * @param inputStream
     * @param accountData
     * @throws IOException
     */
    private void deserializeIgnoredPeople(DataInputStream inputStream, AccountData accountData)
            throws IOException {

        accountData.setIgnoredUserGuids(new ArrayList<>());

        int ignoredPersonCount = inputStream.readInt();
        for (int ii = 0; ii < ignoredPersonCount; ii++) {
            accountData.getIgnoredUserGuids().add(SerializationUtil.readOptionalUTF(inputStream));
        }
    }

    /**
     * Deserialize the account's ignored items.
     *
     * @param inputStream
     * @param accountData
     * @throws IOException
     */
    private void deserializeIgnoredItems(DataInputStream inputStream, AccountData accountData)
            throws IOException {

        accountData.setIgnoredItemRemoteIds(new ArrayList<>());

        int ignoredItemCount = inputStream.readInt();
        for (int ii = 0; ii < ignoredItemCount; ii++) {
            accountData.getIgnoredItemRemoteIds().add(inputStream.readLong());
        }
    }
}
