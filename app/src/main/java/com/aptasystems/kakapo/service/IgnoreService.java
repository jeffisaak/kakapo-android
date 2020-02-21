package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.entities.IgnoredItem;
import com.aptasystems.kakapo.entities.IgnoredPerson;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.util.PrefsUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

@Singleton
public class IgnoreService {

    private EntityDataStore<Persistable> _entityStore;
    private PrefsUtil _prefsUtil;

    @Inject
    public IgnoreService(EntityDataStore<Persistable> entityStore, PrefsUtil prefsUtil) {
        _entityStore = entityStore;
        _prefsUtil = prefsUtil;
    }

    public void ignore(String guid) {
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class,
                _prefsUtil.getCurrentUserAccountId());
        IgnoredPerson ignoredPerson = new IgnoredPerson();
        ignoredPerson.setGuid(guid);
        ignoredPerson.setUserAccount(userAccount);
        _entityStore.insert(ignoredPerson);
    }

    public void ignore(Long itemId) {
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class,
                _prefsUtil.getCurrentUserAccountId());
        IgnoredItem ignoredItem = new IgnoredItem();
        ignoredItem.setItemId(itemId);
        ignoredItem.setUserAccount(userAccount);
        _entityStore.insert(ignoredItem);
    }

    public void unignore(String guid) {
        _entityStore.delete(IgnoredPerson.class)
                .where(IgnoredPerson.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .and(IgnoredPerson.GUID.eq(guid))
                .get()
                .value();
    }

    public void unignore(Long itemId) {
        _entityStore.delete(IgnoredItem.class)
                .where(IgnoredItem.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .and(IgnoredItem.ITEM_ID.eq(itemId))
                .get()
                .value();
    }

    public boolean isIgnored(String guid, Long itemId) {
        return isIgnored(guid) || isIgnored(itemId);
    }

    public boolean isIgnored(String guid) {
        IgnoredPerson ignoredPerson = _entityStore.select(IgnoredPerson.class)
                .where(IgnoredPerson.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .and(IgnoredPerson.GUID.eq(guid))
                .get()
                .firstOrNull();
        return ignoredPerson != null;
    }

    public boolean isIgnored(Long itemId) {
        IgnoredItem ignoredItem = _entityStore.select(IgnoredItem.class)
                .where(IgnoredItem.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .and(IgnoredItem.ITEM_ID.eq(itemId))
                .get()
                .firstOrNull();
        return ignoredItem != null;
    }

}
