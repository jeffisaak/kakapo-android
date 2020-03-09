package com.aptasystems.kakapo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.adapter.model.FriendGroupListItem;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.util.PrefsUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

/**
 * Adapter that provides data to the list of groups on the friend details activity.
 */
public class FriendGroupListAdapter extends ArrayAdapter<FriendGroupListItem> {

    private static final int ROW_LAYOUT_ID = R.layout.row_friend_group;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    PrefsUtil _prefsUtil;

    private long _friendId;

    public FriendGroupListAdapter(Context context, long friendId) {
        super(context, ROW_LAYOUT_ID);
        _friendId = friendId;
        ((KakapoApplication) context.getApplicationContext()).getKakapoComponent().inject(this);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder;

        FriendGroupListItem item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view.
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(ROW_LAYOUT_ID, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set the is-a-member check box value in the row.
        viewHolder.memberCheckBox.setChecked(item.isMember());

        // Set the name of the group in the row.
        viewHolder.groupName.setText(item.getGroupName());

        // Return the completed view to render on screen.
        return convertView;
    }

    /**
     * Refresh the list contents from the group and group members databases.
     */
    public void refresh() {

        // Fetch the list of groups from the database.
        Result<Group> groups = _entityStore.select(Group.class)
                .where(Group.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .orderBy(Group.NAME.asc())
                .get();

        // Iterate over the groups, building list items and adding them to this list.
        List<FriendGroupListItem> friendGroups = new ArrayList<>();
        for (Group group : groups) {

            // See if the friend is a member of the group.
            GroupMember groupMember = _entityStore.select(GroupMember.class)
                    .where(GroupMember.FRIEND_ID.eq(_friendId))
                    .and(GroupMember.GROUP_ID.eq(group.getId()))
                    .get().firstOrNull();

            // Build the list item.
            FriendGroupListItem friendGroupListItem = new FriendGroupListItem(group.getId(), _friendId, group.getName(), groupMember != null);

            // Add the list item to the list of items.
            friendGroups.add(friendGroupListItem);
        }

        // Remove all items from the list and add the new ones.
        clear();
        addAll(friendGroups);
    }

    private static class ViewHolder {
        public View layout;
        public CheckBox memberCheckBox;
        public TextView groupName;

        public ViewHolder(View v) {
            layout = v;
            memberCheckBox = v.findViewById(R.id.member_check_box);
            groupName = v.findViewById(R.id.group_name);
        }
    }
}
