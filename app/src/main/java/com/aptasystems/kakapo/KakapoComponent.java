package com.aptasystems.kakapo;

import com.aptasystems.kakapo.adapter.FriendGroupListAdapter;
import com.aptasystems.kakapo.adapter.FriendRecyclerAdapter;
import com.aptasystems.kakapo.adapter.GroupMemberListAdapter;
import com.aptasystems.kakapo.adapter.GroupRecyclerAdapter;
import com.aptasystems.kakapo.adapter.NewsDetailRecyclerAdapter;
import com.aptasystems.kakapo.adapter.NewsRecyclerAdapter;
import com.aptasystems.kakapo.adapter.QueuedItemRecyclerAdapter;
import com.aptasystems.kakapo.adapter.UserAccountRecyclerAdapter;
import com.aptasystems.kakapo.dao.CachedRegularItemDAO;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.GroupDAO;
import com.aptasystems.kakapo.dao.GroupMemberDAO;
import com.aptasystems.kakapo.dao.IgnoredItemDAO;
import com.aptasystems.kakapo.dao.IgnoredPersonDAO;
import com.aptasystems.kakapo.dao.PreKeyDAO;
import com.aptasystems.kakapo.dao.ShareDAO;
import com.aptasystems.kakapo.dao.ShareRecipientDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.dialog.BaseDialog;
import com.aptasystems.kakapo.fragment.FriendListFragment;
import com.aptasystems.kakapo.fragment.GroupListFragment;
import com.aptasystems.kakapo.fragment.MeFragment;
import com.aptasystems.kakapo.fragment.NewsFragment;
import com.aptasystems.kakapo.service.AccountBackupService;
import com.aptasystems.kakapo.service.AccountRestoreService;
import com.aptasystems.kakapo.service.FriendService;
import com.aptasystems.kakapo.service.GroupService;
import com.aptasystems.kakapo.service.IgnoreService;
import com.aptasystems.kakapo.service.OwnershipService;
import com.aptasystems.kakapo.service.RetrofitWrapper;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.service.UserAccountService;
import com.aptasystems.kakapo.worker.AccountBackupWorker;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {KakapoModule.class})
public interface KakapoComponent {

    void inject(SplashScreenActivity activity);

    void inject(IntroActivity activity);

    void inject(MainActivity activity);

    void inject(SelectUserAccountActivity activity);

    void inject(HelpActivity activity);

    void inject(FriendDetailActivity activity);

    void inject(GroupDetailActivity activity);

    void inject(ShareItemActivity activity);

    void inject(ViewImageActivity activity);

    void inject(NewsItemDetailActivity activity);

    void inject(AccountBackupWorker worker);

    void inject(MeFragment fragment);

    void inject(NewsFragment fragment);

    void inject(FriendListFragment fragment);

    void inject(GroupListFragment fragment);

    void inject(FriendDAO dao);

    void inject(GroupDAO dao);

    void inject(GroupMemberDAO dao);

    void inject(ShareDAO dao);

    void inject(ShareRecipientDAO dao);

    void inject(UserAccountDAO dao);

    void inject(CachedRegularItemDAO dao);

    void inject(PreKeyDAO dao);

    void inject(IgnoredPersonDAO dao);

    void inject(IgnoredItemDAO dao);

    void inject(UserAccountService service);

    void inject(RetrofitWrapper service);

    void inject(ShareService service);

    void inject(IgnoreService service);

    void inject(GroupService service);

    void inject(OwnershipService service);

    void inject(AccountBackupService service);

    void inject(AccountRestoreService service);

    void inject(FriendService service);

    void inject(UserAccountRecyclerAdapter adapter);

    void inject(FriendRecyclerAdapter adapter);

    void inject(GroupRecyclerAdapter adapter);

    void inject(FriendGroupListAdapter adapter);

    void inject(GroupMemberListAdapter adapter);

    void inject(QueuedItemRecyclerAdapter adapter);

    void inject(NewsRecyclerAdapter adapter);

    void inject(NewsDetailRecyclerAdapter adapter);

    void inject(BaseDialog dialog);

}
