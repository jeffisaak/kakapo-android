package com.aptasystems.kakapo.entities;

import java.util.Set;

import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Table;

@Entity
@Table(name="pre_key")
public abstract class AbstractPreKey {

    @Key
    @Generated
    long _id;

    @ForeignKey
    @ManyToOne
    UserAccount _userAccount;

    Long _preKeyId;

    String _publicKey;

    String _secretKey;

    public void setId(long id)
    {
        _id = id;
    }
}
