package com.aptasystems.kakapo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AccountDeletionRequested;
import com.aptasystems.kakapo.event.UserAccountListModelChanged;
import com.aptasystems.kakapo.event.UserAccountSelected;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;

/**
 * Recycler adapter for the sign in/sign up activity.
 */
public class UserAccountRecyclerAdapter extends RecyclerView.Adapter<UserAccountRecyclerAdapter.ViewHolder> {

    @Inject
    EventBus _eventBus;

    @Inject
    UserAccountDAO _userAccountDAO;

    private List<UserAccount> _model;

    public UserAccountRecyclerAdapter(Context context) {
        _model = new ArrayList<>();
        ((KakapoApplication) context.getApplicationContext()).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_user_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final UserAccount entity = _model.get(position);

        String avatarLetter = entity.getName().toUpperCase().substring(0, 1);

        holder.avatarCircleText.setText(avatarLetter);
        holder.avatarCircleImage.setColorFilter(entity.getColour());

        holder.userAccountName.setText(entity.getName());
        holder.userAccountGuid.setText(entity.getGuid());

        holder.deleteAccountButton.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(holder.layout.getContext(), v);
            popupMenu.inflate(R.menu.popup_delete_account);

            MenuItem deleteFromDevice = popupMenu.getMenu().findItem(R.id.action_delete_from_device);
            deleteFromDevice.setOnMenuItemClickListener(menuItem -> {
                _eventBus.post(new AccountDeletionRequested(entity.getId(), false));
                return true;
            });

            MenuItem deleteFromServer = popupMenu.getMenu().findItem(R.id.action_delete_from_server);
            deleteFromServer.setOnMenuItemClickListener(menuItem -> {
                _eventBus.post(new AccountDeletionRequested(entity.getId(), true));
                return true;
            });

            popupMenu.show();
        });

        // Click listener for the row.
        holder.layout.setOnClickListener(view -> _eventBus.post(new UserAccountSelected(entity.getId())));
    }

    @Override
    public int getItemCount() {
        return _model.size();
    }

    /**
     * Refresh the list contents from the user account database.
     */
    public void refresh() {

        // Fetch the user accounts from the data store.
        Result<UserAccount> userAccounts = _userAccountDAO.list();

        // Add the user accounts to the model.
        _model.clear();
        for (UserAccount UserAccount : userAccounts) {
            _model.add(UserAccount);
        }

        // Update the list and post an event.
        notifyDataSetChanged();
        _eventBus.post(new UserAccountListModelChanged(_model.size()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public View layout;
        FrameLayout avatarCircleLayout;
        ImageView avatarCircleImage;
        TextView avatarCircleText;
        TextView userAccountName;
        TextView userAccountGuid;
        ImageButton deleteAccountButton;

        ViewHolder(View v) {
            super(v);
            layout = v;
            avatarCircleLayout = v.findViewById(R.id.avatar_circle_layout);
            avatarCircleImage = v.findViewById(R.id.avatar_circle_image);
            avatarCircleText = v.findViewById(R.id.avatar_circle_text);
            userAccountName = v.findViewById(R.id.user_account_name);
            userAccountGuid = v.findViewById(R.id.user_account_guid);
            deleteAccountButton = v.findViewById(R.id.delete_account_button);
        }
    }
}
