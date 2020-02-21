package com.aptasystems.kakapo;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.aptasystems.kakapo.adapter.GroupMemberListAdapter;
import com.aptasystems.kakapo.adapter.model.GroupMemberListItem;
import com.aptasystems.kakapo.dialog.RenameGroupDialog;
import com.aptasystems.kakapo.service.GroupService;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.event.GroupRenamed;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

public class GroupDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "groupId";

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    EventBus _eventBus;

    @Inject
    GroupService _groupService;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    @BindView(R.id.layout_coordinator)
    CoordinatorLayout _coordinatorLayout;

    @BindView(R.id.list_view_group_members)
    ListView _listView;

    @BindView(R.id.text_view_no_friends)
    TextView _noItemsTextView;

    private GroupMemberListAdapter _listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        setContentView(R.layout.activity_group_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ButterKnife.bind(this);

        updateUserInterface();

        final long groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 0L);

        // Set up the list view.
        _listAdapter = new GroupMemberListAdapter(this, groupId);
        _listView.setAdapter(_listAdapter);
        _listView.setOnItemClickListener((parent, view, position, id) -> {
            GroupMemberListItem item = _listAdapter.getItem(position);
            item.setMember(!item.isMember());
            _listAdapter.notifyDataSetChanged();

            if (item.isMember()) {
                Friend friend = _entityStore.findByKey(Friend.class, item.getFriendId());
                Group group = _entityStore.findByKey(Group.class, item.getGroupId());
                GroupMember groupMember = new GroupMember();
                groupMember.setFriend(friend);
                groupMember.setGroup(group);
                _entityStore.insert(groupMember);
            } else {
                _entityStore.delete(GroupMember.class)
                        .where(GroupMember.FRIEND_ID.eq(item.getFriendId()))
                        .and(GroupMember.GROUP_ID.eq(item.getGroupId()))
                        .get()
                        .value();
            }
        });
        _listView.setEmptyView(_noItemsTextView);
        _listAdapter.refresh();
    }

    private void updateUserInterface() {
        // Set the title to the name of the group.
        Long groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 0L);
        Group group = _entityStore.findByKey(Group.class, groupId);
        String title = String.format(getString(R.string.group_detail_title_activity), group.getName());
        setTitle(title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register to listen for events.
        if (!_eventBus.isRegistered(this)) {
            _eventBus.register(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(GroupRenamed event) {
        updateUserInterface();
    }

    /**
     * Rename a group; show the rename group dialog. Invoked by a menu item.
     *
     * @param menuItem
     */
    public void renameGroup(MenuItem menuItem) {
        final Long groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 0L);
        RenameGroupDialog dialog = RenameGroupDialog.newInstance(groupId);
        dialog.show(getSupportFragmentManager(), "renameGroupDialog");
    }

    /**
     * Delete a group. Invoked by a menu item.
     *
     * @param menuItem
     */
    public void deleteGroup(MenuItem menuItem) {

        _confirmationDialogUtil.showConfirmationDialog(getSupportFragmentManager(),
                R.string.dialog_confirm_title_delete_group,
                R.string.dialog_confirm_text_delete_group,
                "deleteGroupConfirmation",
                () -> {
                    // Delete the group.
                    final Long groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 0L);
                    Group group = _entityStore.findByKey(Group.class, groupId);
                    _groupService.deleteGroup(group);

                    // Finish the activity.
                    finish();
                });

    }
}
