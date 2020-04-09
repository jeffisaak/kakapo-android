package com.aptasystems.kakapo.fragment;

import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

public abstract class BaseFragment extends Fragment {

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    ShareService _shareItemService;

    @Inject
    EventBus _eventBus;

}
