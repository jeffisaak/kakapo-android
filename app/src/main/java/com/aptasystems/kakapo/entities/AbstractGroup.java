package com.aptasystems.kakapo.entities;

import java.util.Set;

import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToMany;
import io.requery.ManyToOne;
import io.requery.Table;

@Entity
@Table(name = "friend_group")
public abstract class AbstractGroup {

    @Key
    @Generated
    long _id;

    @ForeignKey
    @ManyToOne
    UserAccount _userAccount;

    @ManyToMany
    Set<Friend> _friends;

    String _name;

    public void setId(long id) {
        _id = id;
    }
}
