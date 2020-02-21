package com.aptasystems.kakapo.entities;

import java.util.Set;

import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.JunctionTable;
import io.requery.Key;
import io.requery.ManyToMany;
import io.requery.ManyToOne;
import io.requery.Table;

@Entity
@Table(name = "friend")
public abstract class AbstractFriend {

    @Key
    @Generated
    long _id;

    @ForeignKey
    @ManyToOne
    UserAccount _userAccount;

    @ManyToMany
    @JunctionTable(type = AbstractGroupMember.class)
    Set<Group> _groups;

    String _guid;

    String _name;

    byte[] _publicKeyRingsData;

    int _colour;

    public void setId(long id) {
        _id = id;
    }

}
