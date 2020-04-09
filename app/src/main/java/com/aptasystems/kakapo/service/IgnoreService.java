package com.aptasystems.kakapo.service;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.dao.IgnoredItemDAO;
import com.aptasystems.kakapo.dao.IgnoredPersonDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
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

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    IgnoredPersonDAO _ignoredPersonDAO;

    @Inject
    IgnoredItemDAO _ignoredItemDAO;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    public IgnoreService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public void ignore(String guid) {
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
        _ignoredPersonDAO.insert(userAccount, guid);
    }

    public void ignore(Long itemRemoteId) {
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
        _ignoredItemDAO.insert(userAccount, itemRemoteId);
    }

    public void unignore(String guid) {
        _ignoredPersonDAO.delete(_prefsUtil.getCurrentUserAccountId(), guid);
    }

    public void unignore(Long itemRemoteId) {
        _ignoredItemDAO.delete(_prefsUtil.getCurrentUserAccountId(), itemRemoteId);
    }

    public boolean isIgnored(String guid, Long itemRemoteId) {
        return isIgnored(guid) || isIgnored(itemRemoteId);
    }

    public boolean isIgnored(String guid) {
        IgnoredPerson ignoredPerson = _ignoredPersonDAO.find(_prefsUtil.getCurrentUserAccountId(), guid);
        return ignoredPerson != null;
    }

    public boolean isIgnored(Long itemRemoteId) {
        IgnoredItem ignoredItem = _ignoredItemDAO.find(_prefsUtil.getCurrentUserAccountId(), itemRemoteId);
        return ignoredItem != null;
    }

}
