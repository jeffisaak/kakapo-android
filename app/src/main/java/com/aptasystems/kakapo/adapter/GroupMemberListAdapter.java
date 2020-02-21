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
import com.aptasystems.kakapo.adapter.model.GroupMemberListItem;
import com.aptasystems.kakapo.entities.Friend;
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
 * Adapter that provides data to the list of friends on the group details activity.
 */
public class GroupMemberListAdapter extends ArrayAdapter<GroupMemberListItem> {

    private static final int ROW_LAYOUT_ID = R.layout.row_group_member;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    PrefsUtil _prefsUtil;

    private long _groupId;

    public GroupMemberListAdapter(Context context, long groupId) {
        super(context, ROW_LAYOUT_ID);
        _groupId = groupId;
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

        GroupMemberListItem item = getItem(position);

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

        // Set the name and guid of the friend in the row.
        viewHolder.friendNameTextView.setText(item.getFriendName());
        viewHolder.friendGuidTextView.setText(item.getFriendGuid());

        // Return the completed view to render on screen.
        return convertView;
    }

    /**
     * Refresh the list contents from the group and group members databases.
     */
    public void refresh() {

        // Fetch the list of friends from the database.
        Result<Friend> friends = _entityStore.select(Friend.class)
                .where(Friend.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .get();

        // Iterate over the friends, building list items and adding them to this list.
        List<GroupMemberListItem> groupMembers = new ArrayList<>();
        for (Friend friend : friends) {

            // See if the friend is a member of the group.
            GroupMember groupMember = _entityStore.select(GroupMember.class)
                    .where(GroupMember.FRIEND_ID.eq(friend.getId()))
                    .and(GroupMember.GROUP_ID.eq(_groupId))
                    .get().firstOrNull();

            // Build the list item.
            GroupMemberListItem groupMemberListItem = new GroupMemberListItem(_groupId, friend.getId(), friend.getName(), friend.getGuid(), groupMember != null);
            groupMembers.add(groupMemberListItem);
        }

        // Remove all items from the list and add the new ones.
        clear();
        addAll(groupMembers);
    }

    private static class ViewHolder {
        public View layout;
        public CheckBox memberCheckBox;
        public TextView friendNameTextView;
        public TextView friendGuidTextView;

        public ViewHolder(View v) {
            layout = v;
            memberCheckBox = v.findViewById(R.id.check_box_is_member);
            friendNameTextView = v.findViewById(R.id.text_view_friend_name);
            friendGuidTextView = v.findViewById(R.id.text_view_friend_guid);
        }


    }
}
