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
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.GroupMemberDAO;
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
    FriendDAO _friendDAO;

    @Inject
    GroupMemberDAO _groupMemberDAO;

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
        viewHolder.friendName.setText(item.getFriendName());
        viewHolder.friendGuid.setText(item.getFriendGuid());

        // Return the completed view to render on screen.
        return convertView;
    }

    /**
     * Refresh the list contents from the group and group members databases.
     */
    public void refresh() {

        // Fetch the list of friends from the database.
        Result<Friend> friends = _friendDAO.list(_prefsUtil.getCurrentUserAccountId());

        // Iterate over the friends, building list items and adding them to this list.
        List<GroupMemberListItem> groupMembers = new ArrayList<>();
        for (Friend friend : friends) {

            // See if the friend is a member of the group.
            GroupMember groupMember = _groupMemberDAO.find(friend.getId(), _groupId);

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
        public TextView friendName;
        public TextView friendGuid;

        public ViewHolder(View v) {
            layout = v;
            memberCheckBox = v.findViewById(R.id.member_check_box);
            friendName = v.findViewById(R.id.friend_name);
            friendGuid = v.findViewById(R.id.friend_guid);
        }


    }
}
