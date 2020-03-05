package com.aptasystems.kakapo;

import com.aptasystems.kakapo.adapter.FriendGroupListAdapter;
import com.aptasystems.kakapo.adapter.FriendRecyclerAdapter;
import com.aptasystems.kakapo.adapter.GroupMemberListAdapter;
import com.aptasystems.kakapo.adapter.GroupRecyclerAdapter;
import com.aptasystems.kakapo.adapter.NewsDetailRecyclerAdapter;
import com.aptasystems.kakapo.adapter.NewsRecyclerAdapter;
import com.aptasystems.kakapo.adapter.QueuedItemRecyclerAdapter;
import com.aptasystems.kakapo.adapter.UserAccountRecyclerAdapter;
import com.aptasystems.kakapo.dialog.BaseDialog;
import com.aptasystems.kakapo.fragment.FriendListFragment;
import com.aptasystems.kakapo.fragment.GroupListFragment;
import com.aptasystems.kakapo.fragment.MeFragment;
import com.aptasystems.kakapo.fragment.NewsFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={KakapoModule.class})
public interface KakapoComponent {

    void inject(SplashScreenActivity activity);
    void inject(IntroActivity activity);
    void inject(MainActivity activity);
    void inject(SelectUserAccountActivity activity);
    void inject(HelpActivity activity);
    void inject(RestoreAccountActivity activity);
    void inject(FriendDetailActivity activity);
    void inject(GroupDetailActivity activity);
    void inject(ShareItemActivity activity);
    void inject(ViewImageActivity activity);
    void inject(NewsItemDetailActivity activity);

    void inject(MeFragment fragment);
    void inject(NewsFragment fragment);
    void inject(FriendListFragment fragment);
    void inject(GroupListFragment fragment);

//    void inject(WebSocketService service);

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
