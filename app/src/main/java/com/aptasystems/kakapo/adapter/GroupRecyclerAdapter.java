package com.aptasystems.kakapo.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.aptasystems.kakapo.GroupDetailActivity;
import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.service.GroupService;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.GroupsListModelChanged;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

public class GroupRecyclerAdapter extends RecyclerView.Adapter<GroupRecyclerAdapter.ViewHolder> {

    private static final String TAG = GroupRecyclerAdapter.class.getSimpleName();

    @Inject
    EventBus _eventBus;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    GroupService _groupService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    private FragmentActivity _activity;
    private List<Group> _model;

    public GroupRecyclerAdapter(FragmentActivity activity) {
        _activity = activity;
        _model = new ArrayList<>();
        ((KakapoApplication) activity.getApplicationContext()).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Group entity = _model.get(position);

        String avatarLetter = entity.getName().toUpperCase().substring(0, 1);
        int colour = holder.layout.getContext().getResources().getColor(R.color.groupAvatarColour);

        holder.layout.setSelected(false);
        holder.avatarCircleTextView.setText(avatarLetter);
        holder.avatarCircleImageView.setColorFilter(colour);
        holder.groupNameTextView.setText(entity.getName());

        // Format the "x friends in this group" string for display.
        int groupMemberCount = _entityStore.count(GroupMember.class)
                .where(GroupMember.GROUP_ID.eq(entity.getId())).get().value();
        String countText = String.format(holder.layout.getContext().getString(R.string.fragment_groups_label_friend_count_in_group), groupMemberCount);
        holder.memberCountTextView.setText(countText);

        // Delete the group.
        holder.deleteGroupImageButton.setOnClickListener(v -> _confirmationDialogUtil.showConfirmationDialog(_activity.getSupportFragmentManager(),
                R.string.dialog_confirm_title_delete_group,
                R.string.dialog_confirm_text_delete_group,
                "deleteGroupConfirmation",
                () -> {
                    _groupService.deleteGroup(entity);
                    refresh();
                }));

        // Click listener for the row - opens the group details activity.
        holder.layout.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), GroupDetailActivity.class);
            intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, entity.getId());
            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return _model.size();
    }

    public void refresh() {

        // Fetch the groups from the data store.
        UserAccount userAccount = _entityStore.findByKey(UserAccount.class, _prefsUtil.getCurrentUserAccountId());
        Result<Group> groups = _entityStore.select(Group.class)
                .where(Group.USER_ACCOUNT.eq(userAccount))
                .orderBy(Group.NAME.asc())
                .get();

        // Add the groups to the model.
        _model.clear();
        for (Group Group : groups) {
            _model.add(Group);
        }

        notifyDataSetChanged();

        _eventBus.post(new GroupsListModelChanged(_model.size()));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        public FrameLayout avatarCircleLayout;
        public ImageView avatarCircleImageView;
        public TextView avatarCircleTextView;
        public TextView groupNameTextView;
        public TextView memberCountTextView;
        public ImageButton deleteGroupImageButton;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            avatarCircleLayout = v.findViewById(R.id.frame_layout_avatar_circle);
            avatarCircleImageView = v.findViewById(R.id.image_view_avatar_circle);
            avatarCircleTextView = v.findViewById(R.id.text_view_avatar_circle);
            groupNameTextView = v.findViewById(R.id.text_view_group_name);
            memberCountTextView = v.findViewById(R.id.text_view_member_count);
            deleteGroupImageButton = v.findViewById(R.id.image_button_delete_group);
        }
    }
}
