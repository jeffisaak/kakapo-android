package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

@Singleton
public class GroupService {

    private EntityDataStore<Persistable> _entityStore;

    @Inject
    public GroupService(EntityDataStore<Persistable> entityStore) {
        _entityStore = entityStore;
    }

    public void deleteGroup(Group group) {

        // Delete any group member entities that reference this group.
        _entityStore.delete(GroupMember.class)
                .where(GroupMember.GROUP_ID.eq(group.getId()))
                .get()
                .value();

        // Delete the group.
        _entityStore.delete(group);
    }

}
