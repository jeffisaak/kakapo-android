package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.entities.IgnoredItem;
import com.aptasystems.kakapo.entities.IgnoredPerson;
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
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, userAccountId);
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
        serializeGroups(dataOutputStream, userAccount.getGroups());
        serializeFriends(dataOutputStream, userAccount.getFriends());
        serializeGroupMembers(dataOutputStream, allGroupMembers);
        serializeIgnoredPeople(dataOutputStream, ignoredPeople.toList());
        serializeIgnoredItems(dataOutputStream, ignoredItems.toList());

        // Close streams.
        dataOutputStream.flush();
        dataOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    private void serializeUserAccount(DataOutputStream outputStream, UserAccount userAccount)
            throws IOException {
        outputStream.writeLong(userAccount.getId());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getGuid());
        SerializationUtil.writeOptionalUTF(outputStream, userAccount.getName());
        SerializationUtil.writeBytes(outputStream, userAccount.getSecretKeyRings());
        SerializationUtil.writeBytes(outputStream, userAccount.getPublicKeyRings());
        outputStream.writeInt(userAccount.getColour());
    }

    private void serializeGroups(DataOutputStream outputStream, Collection<Group> groups)
            throws IOException {
        outputStream.writeInt(groups.size());
        for (Group group : groups) {
            outputStream.writeLong(group.getId());
            SerializationUtil.writeOptionalUTF(outputStream, group.getName());
        }
    }

    private void serializeFriends(DataOutputStream outputStream,
                                  Collection<Friend> friends) throws IOException {
        outputStream.writeInt(friends.size());
        for (Friend friend : friends) {
            outputStream.writeLong(friend.getId());
            SerializationUtil.writeOptionalUTF(outputStream, friend.getGuid());
            SerializationUtil.writeOptionalUTF(outputStream, friend.getName());
            SerializationUtil.writeBytes(outputStream, friend.getPublicKeyRingsData());
            outputStream.writeInt(friend.getColour());
        }
    }

    private void serializeGroupMembers(DataOutputStream outputStream,
                                       Collection<GroupMember> groupMembers) throws IOException {
        outputStream.writeInt(groupMembers.size());
        for (GroupMember groupMember : groupMembers) {
            outputStream.writeLong(groupMember.getFriend().getId());
            outputStream.writeLong(groupMember.getGroup().getId());
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
            outputStream.writeLong(ignoredItem.getItemId());
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
        deserializeGroups(dataInputStream, accountData);
        deserializeFriends(dataInputStream, accountData);
        deserializeGroupMembers(dataInputStream, accountData);
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

        UserAccount userAccount = new UserAccount();

        userAccount.setId(inputStream.readLong());
        userAccount.setGuid(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setName(SerializationUtil.readOptionalUTF(inputStream));
        userAccount.setSecretKeyRings(SerializationUtil.readBytes(inputStream));
        userAccount.setPublicKeyRings(SerializationUtil.readBytes(inputStream));
        userAccount.setColour(inputStream.readInt());

        accountData.setUserAccount(userAccount);
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
            Group group = new Group();
            group.setId(inputStream.readLong());
            group.setName(SerializationUtil.readOptionalUTF(inputStream));
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
            Friend friend = new Friend();
            friend.setId(inputStream.readLong());
            friend.setGuid(SerializationUtil.readOptionalUTF(inputStream));
            friend.setName(SerializationUtil.readOptionalUTF(inputStream));
            friend.setPublicKeyRingsData(SerializationUtil.readBytes(inputStream));
            friend.setColour(inputStream.readInt());
            accountData.getFriends().add(friend);
        }
    }

    /**
     * Deserialize the account's group members.
     *
     * @param inputStream
     * @param accountData
     * @throws IOException
     */
    private void deserializeGroupMembers(DataInputStream inputStream, AccountData accountData)
            throws IOException {

        accountData.setGroupMembers(new ArrayList<>());

        int groupMemberCount = inputStream.readInt();
        for (int ii = 0; ii < groupMemberCount; ii++) {
            long friendId = inputStream.readLong();
            long groupId = inputStream.readLong();
            AccountRestoreService.GroupMemberMapping groupMember = new AccountRestoreService.GroupMemberMapping();
            groupMember.setFriendId(friendId);
            groupMember.setGroupId(groupId);
            accountData.getGroupMembers().add(groupMember);
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

        accountData.setIgnoredPeople(new ArrayList<>());

        int ignoredPersonCount = inputStream.readInt();
        for (int ii = 0; ii < ignoredPersonCount; ii++) {

            IgnoredPerson ignoredPerson = new IgnoredPerson();
            ignoredPerson.setGuid(SerializationUtil.readOptionalUTF(inputStream));

            accountData.getIgnoredPeople().add(ignoredPerson);
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

        accountData.setIgnoredItems(new ArrayList<>());

        int ignoredItemCount = inputStream.readInt();
        for (int ii = 0; ii < ignoredItemCount; ii++) {

            IgnoredItem ignoredItem = new IgnoredItem();
            ignoredItem.setItemId(inputStream.readLong());

            accountData.getIgnoredItems().add(ignoredItem);
        }
    }
}
