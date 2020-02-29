package com.aptasystems.kakapo.adapter;

import android.app.Activity;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.MainActivity;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.entities.Share;
import com.aptasystems.kakapo.entities.ShareState;
import com.aptasystems.kakapo.event.QueuedItemDeleted;
import com.aptasystems.kakapo.event.QueuedItemsListModelChanged;
import com.aptasystems.kakapo.util.PrefsUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import kakapo.util.TimeUtil;

public class QueuedItemRecyclerAdapter
        extends RecyclerView.Adapter<QueuedItemRecyclerAdapter.ViewHolder> {

    @Inject
    EventBus _eventBus;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    ShareService _shareItemService;

    @Inject
    PrefsUtil _prefsUtil;

    private Activity _activity;
    private List<Share> _model;

    public QueuedItemRecyclerAdapter(Activity activity) {
        _activity = activity;
        _model = new ArrayList<>();
        ((KakapoApplication) activity.getApplicationContext()).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_queued_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Share entity = _model.get(position);

        holder.layout.setSelected(false);

        holder.layoutErrorStatus.setVisibility(
                entity.getState() == ShareState.Error ? View.VISIBLE : View.GONE);
        holder.layoutQueuedStatus.setVisibility(
                entity.getState() == ShareState.Queued ? View.VISIBLE : View.GONE);
        holder.layoutSubmittingStatus.setVisibility(
                entity.getState() == ShareState.Submitting ? View.VISIBLE : View.GONE);

        switch (entity.getType()) {
            case RegularV1:
                holder.itemTitleTextView.setText(entity.getTitle());
                break;
            case ResponseV1:
                holder.itemTitleTextView.setText(entity.getMessage());
                break;
        }

        String timestamp = DateUtils.formatDateTime(_activity,
                entity.getTimestampGmt(),
                DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_YEAR |
                        DateUtils.FORMAT_SHOW_TIME);
        String timestampText = String.format(_activity.getString(R.string.fragment_me_queued_item_row_creation_date), timestamp);
        holder.itemTimestampTextView.setText(timestampText);

        // Long click listener for the row. Show a popup menu.
        holder.layout.setOnLongClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(holder.layout.getContext(), holder.popupMenuAnchor);
            popupMenu.inflate(R.menu.popup_queued_item);

            // Enablement and on click listener for resubmit item.
            MenuItem resubmitQueuedItem =
                    popupMenu.getMenu().findItem(R.id.action_resubmit_queued_item);
            resubmitQueuedItem.setEnabled(entity.getState() == ShareState.Error);
            resubmitQueuedItem.setOnMenuItemClickListener(menuItem -> {
                _shareItemService.submitItemAsync(MainActivity.class,
                        entity.getId(),
                        _prefsUtil.getCurrentHashedPassword());
                return true;
            });

            // Enablement and on click listener for delete item.
            MenuItem deleteQueuedItem =
                    popupMenu.getMenu().findItem(R.id.action_deleted_queued_item);
            deleteQueuedItem.setEnabled(entity.getState() == ShareState.Error);
            deleteQueuedItem.setOnMenuItemClickListener(menuItem -> {
                _shareItemService.deleteQueuedItem(entity.getId());
                _eventBus.post(new QueuedItemDeleted());
                refresh();
                return true;
            });

            popupMenu.show();
            return true;
        });

        holder.layout.setOnClickListener(v -> {
            // Only respond to the click if in error state.
            if (entity.getState() == ShareState.Error) {
                _shareItemService.showShareItemActivity(_activity, entity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return _model.size();
    }

    public void refresh() {

        // Fetch the queued items from the data store.
        Result<Share> shareItems = _entityStore.select(Share.class)
                .where(Share.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .orderBy(Share.TIMESTAMP_GMT.asc())
                .get();

        // Add the queued items to the model.
        _model.clear();
        for (Share shareItem : shareItems) {
            _model.add(shareItem);
        }

        notifyDataSetChanged();

        _eventBus.post(new QueuedItemsListModelChanged(_model.size()));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        FrameLayout layoutQueuedStatus;
        FrameLayout layoutErrorStatus;
        FrameLayout layoutSubmittingStatus;
        TextView itemTitleTextView;
        TextView itemTimestampTextView;
        FrameLayout popupMenuAnchor;

        ViewHolder(View v) {
            super(v);
            layout = v;
            layoutQueuedStatus = v.findViewById(R.id.layout_queued_status);
            layoutErrorStatus = v.findViewById(R.id.layout_error_status);
            layoutSubmittingStatus = v.findViewById(R.id.layout_submitting_status);
            itemTitleTextView = v.findViewById(R.id.text_view_queued_item_title);
            itemTimestampTextView = v.findViewById(R.id.text_view_queued_item_timestamp);
            popupMenuAnchor = v.findViewById(R.id.popup_menu_anchor);
        }
    }
}
