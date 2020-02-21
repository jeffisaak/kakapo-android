package com.aptasystems.kakapo.entities;

import java.util.Set;

import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.OneToMany;
import io.requery.Table;

@Entity
@Table(name="user_account")
public abstract class AbstractUserAccount {

    @Key
    @Generated
    long _id;

    String _guid;

    String _name;

    byte[] _publicKeyRings;

    byte[] _secretKeyRings;

    Integer _colour;

    public void setId(long id)
    {
        _id = id;
    }

    @OneToMany
    Set<Group> _groups;

    @OneToMany
    Set<Friend> _friends;

    @OneToMany
    Set<IgnoredPerson> _ignoredPeople;

    @OneToMany
    Set<Share> _shareItems;
}
