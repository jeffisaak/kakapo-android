package com.aptasystems.kakapo.entities;

import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Table;

@Entity
@Table(name="ignored_person")
public abstract class AbstractIgnoredPerson {

    @Key
    @Generated
    long _id;

    @ForeignKey
    @ManyToOne
    UserAccount _userAccount;

    String _guid;

    public void setId(long id)
    {
        _id = id;
    }

}
