package com.aptasystems.kakapo.entities;

import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.Table;

@Entity
@Table(name="group_member")
public abstract class AbstractGroupMember {

    @Key
    @ForeignKey(references = AbstractGroup.class, referencedColumn = "_id")
    @ManyToOne
    Group _group;

    @Key
    @ForeignKey(references = AbstractFriend.class, referencedColumn = "_id")
    @ManyToOne
    Friend _friend;

}
