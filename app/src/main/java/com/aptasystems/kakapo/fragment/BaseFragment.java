package com.aptasystems.kakapo.fragment;

import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    ShareService _shareItemService;

    @Inject
    EventBus _eventBus;

}
