package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.GroupDAO;
import com.aptasystems.kakapo.dao.GroupMemberDAO;
import com.aptasystems.kakapo.entities.Group;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GroupService {

    @Inject
    GroupMemberDAO _groupMemberDAO;

    @Inject
    GroupDAO _groupDAO;

    @Inject
    public GroupService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public void deleteGroup(Group group) {

        // Delete any group member entities that reference this group.
        _groupMemberDAO.deleteForGroup(group);

        // Delete the group.
        _groupDAO.delete(group);
    }

}
