package com.aptasystems.kakapo.fragment;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.Unbinder;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

public abstract class BaseFragment extends Fragment {

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    ShareService _shareItemService;

    @Inject
    EventBus _eventBus;

    Unbinder _unbinder;

    @BindView(R.id.layout_coordinator)
    CoordinatorLayout _coordinatorLayout;

}
