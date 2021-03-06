package com.aptasystems.kakapo.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.aptasystems.kakapo.FriendDetailActivity;
import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.service.FriendService;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.event.FriendListModelChanged;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import io.requery.query.Result;

public class FriendRecyclerAdapter extends RecyclerView.Adapter<FriendRecyclerAdapter.ViewHolder> {

    private static final String TAG = FriendRecyclerAdapter.class.getSimpleName();

    @Inject
    EventBus _eventBus;

    @Inject
    FriendDAO _friendDAO;

    @Inject
    FriendService _friendService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    private FragmentActivity _activity;
    private List<Friend> _model;

    public FriendRecyclerAdapter(FragmentActivity activity) {
        _activity = activity;
        _model = new ArrayList<>();
        ((KakapoApplication) activity.getApplicationContext()).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Friend entity = _model.get(position);

        String avatarLetter = entity.getName().toUpperCase().substring(0, 1);

        holder.layout.setSelected(false);
        holder.avatarCircleTextView.setText(avatarLetter);
        holder.avatarCircleImage.setColorFilter(entity.getColour());
        holder.friendName.setText(entity.getName());
        holder.friendGuid.setText(entity.getGuid());

        holder.deleteFriendButton.setOnClickListener(v -> _confirmationDialogUtil.showConfirmationDialog(_activity.getSupportFragmentManager(),
                R.string.dialog_confirm_title_delete_friend,
                R.string.dialog_confirm_text_delete_friend,
                "deleteFriendConfirmation",
                () -> {
                    _friendService.deleteFriend(entity);
                    refresh();
                }));

        // Click listener for the row.
        holder.layout.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), FriendDetailActivity.class);
            intent.putExtra(FriendDetailActivity.EXTRA_FRIEND_ID, entity.getId());
            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return _model.size();
    }

    public void refresh() {

        // Fetch the user accounts from the data store.

        Result<Friend> friends = _friendDAO.list(_prefsUtil.getCurrentUserAccountId());

        // Clear the model, then add the user accounts to the model.
        _model.clear();
        for (Friend Friend : friends) {
            _model.add(Friend);
        }
        notifyDataSetChanged();

        _eventBus.post(new FriendListModelChanged(_model.size()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        FrameLayout avatarCircleLayout;
        ImageView avatarCircleImage;
        TextView avatarCircleTextView;
        TextView friendName;
        TextView friendGuid;
        ImageButton deleteFriendButton;

        ViewHolder(View v) {
            super(v);
            layout = v;
            avatarCircleLayout = v.findViewById(R.id.avatar_circle_layout);
            avatarCircleImage = v.findViewById(R.id.avatar_circle_image);
            avatarCircleTextView = v.findViewById(R.id.avatar_circle_text);
            friendName = v.findViewById(R.id.friend_name);
            friendGuid = v.findViewById(R.id.friend_guid);
            deleteFriendButton = v.findViewById(R.id.delete_friend_button);
        }
    }
}
