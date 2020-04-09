package com.aptasystems.kakapo.entities;

import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Table;

@Entity
@Table(name = "share_recipient")
public class AbstractShareRecipient {

    @Key
    @Generated
    long _id;

    @ForeignKey
    @ManyToOne
    Share _item;

    String _guid;

    String _signingPublicKey;

    Long _preKeyId;

    String _preKey;

    public void setId(long id) {
        _id = id;
    }

}
