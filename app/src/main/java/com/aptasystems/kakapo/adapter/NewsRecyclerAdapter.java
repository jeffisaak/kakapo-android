package com.aptasystems.kakapo.adapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.NewsItemDetailActivity;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.ViewImageActivity;
import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.adapter.model.NewsListItemState;
import com.aptasystems.kakapo.adapter.model.RegularNewsListItem;
import com.aptasystems.kakapo.adapter.model.ResponseNewsListItem;
import com.aptasystems.kakapo.event.AddFriendRequested;
import com.aptasystems.kakapo.event.FetchItemHeadersRequested;
import com.aptasystems.kakapo.event.NewsFilterApplied;
import com.aptasystems.kakapo.event.QueuedItemDeleted;
import com.aptasystems.kakapo.service.FriendService;
import com.aptasystems.kakapo.service.IgnoreService;
import com.aptasystems.kakapo.service.OwnedBy;
import com.aptasystems.kakapo.service.OwnershipInfo;
import com.aptasystems.kakapo.service.OwnershipService;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;
import com.aptasystems.kakapo.util.FilterType;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.view.DoubleClickPreventingOnClickListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import kakapo.util.TimeUtil;

public class NewsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @Inject
    ShareService _shareItemService;

    @Inject
    EventBus _eventBus;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    FriendService _friendService;

    @Inject
    IgnoreService _ignoreService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    OwnershipService _ownershipService;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    private FragmentActivity _activity;
    private List<AbstractNewsListItem> _allItems;
    private List<AbstractNewsListItem> _filteredItems;
    private long _remainingItemCount = -1L;

    private static final int VIEW_TYPE_REGULAR = 0;
    private static final int VIEW_TYPE_FOOTER = 1;

    public NewsRecyclerAdapter(FragmentActivity activity) {
        _activity = activity;
        _allItems = new ArrayList<>();
        _filteredItems = new ArrayList<>();
        ((KakapoApplication) activity.getApplicationContext()).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_FOOTER:
                return new FooterViewHolder(inflater.inflate(R.layout.row_news_footer, parent, false));
            case VIEW_TYPE_REGULAR:
            default:
                return new RegularViewHolder(inflater.inflate(R.layout.row_news, parent, false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= _filteredItems.size()) {
            return VIEW_TYPE_FOOTER;
        }
        return VIEW_TYPE_REGULAR;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof RegularViewHolder) {
            final AbstractNewsListItem entity = _filteredItems.get(position);
            bindViewHolder((RegularViewHolder) holder, entity);
        } else if (holder instanceof FooterViewHolder) {
            bindViewHolder((FooterViewHolder) holder);
        }
    }

    private void bindViewHolder(RegularViewHolder holder, AbstractNewsListItem entity) {

        OwnershipInfo ownershipInfo = _ownershipService.getOwnership(entity);

        holder.layout.setSelected(false);
        holder.avatarCircleTextView.setText(ownershipInfo.getAvatarLetter());
        holder.avatarCircleImageView.setColorFilter(ownershipInfo.getColour());

        // Set all our text views gone and then based on the state, show some things.
        holder.decryptionFailedTextView.setVisibility(View.GONE);
        holder.deserializationFailedTextView.setVisibility(View.GONE);
        holder.decryptingTextView.setVisibility(View.GONE);
        holder.itemTitleTextView.setVisibility(View.GONE);
        holder.statusTextView.setVisibility(View.GONE);
        switch (entity.getState()) {
            case Decrypting:
                holder.decryptingTextView.setVisibility(View.VISIBLE);
                break;
            case Decrypted:
            case Deleted:
                holder.itemTitleTextView.setVisibility(View.VISIBLE);
                break;
            case Queued:
                holder.itemTitleTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setText(_activity.getString(R.string.app_text_deleted_item));
                break;
            case Submitting:
                holder.itemTitleTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setText(_activity.getString(R.string.fragment_news_encrypting_and_uploading));
                break;
            case SubmissionError:
                holder.itemTitleTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setText(_activity.getString(R.string.fragment_news_upload_failed));
                break;
            case DecryptionFailed:
                holder.decryptionFailedTextView.setVisibility(View.VISIBLE);
                break;
            case DeserializationFailed:
                holder.deserializationFailedTextView.setVisibility(View.VISIBLE);
                break;
        }

        RegularNewsListItem newsListItem = (RegularNewsListItem) entity;

        // Set the title.
        if (newsListItem.getState() == NewsListItemState.Deleted) {
            holder.itemTitleTextView.setText(_activity.getString(R.string.app_text_deleted_item));
        } else if (newsListItem.getTitle() != null) {
            holder.itemTitleTextView.setText(newsListItem.getTitle());
        }

        if (newsListItem.getThumbnailData() != null) {

            // If we have a thumbnail, create a bitmap and show it.
            Bitmap thumbnail = BitmapFactory.decodeByteArray(newsListItem.getThumbnailData(),
                    0,
                    newsListItem.getThumbnailData().length);
            holder.thumbnailImageView.setImageBitmap(thumbnail);
            holder.thumbnailImageView.setVisibility(View.VISIBLE);

            holder.thumbnailImageView.setOnClickListener(new DoubleClickPreventingOnClickListener() {

                @Override
                public void onClickInternal(View view) {

                    if (entity.getLocalId() != null && entity.getState() == NewsListItemState.SubmissionError) {

                        // If this is a local (queued) item in submission error state,
                        // do the thing.
                        _shareItemService.showShareItemActivity(_activity, entity.getLocalId());

                    } else if (entity.getState() == NewsListItemState.Decrypted) {

                        // If it's in decrypted state, do the other thing.
                        Intent intent = new Intent(_activity, ViewImageActivity.class);
                        intent.putExtra(ViewImageActivity.EXTRA_ITEM_REMOTE_ID, entity.getRemoteId());
                        view.getContext().startActivity(intent);
                    }
                }
            });

        } else {

            // We don't have a thumbnail, so hide it.
            holder.thumbnailImageView.setImageBitmap(null);
            holder.thumbnailImageView.setVisibility(View.GONE);
            holder.thumbnailImageView.setOnClickListener(null);
            holder.thumbnailImageView.setClickable(false);
        }

        // Convert the item's timestamp (which is in GMT) to local time and format it for display.
        long timestampInZulu = TimeUtil.timestampInZulu(entity.getItemTimestamp());
        String timestamp = DateUtils.formatDateTime(_activity,
                timestampInZulu,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME);
        String timestampText = String.format(_activity.getString(R.string.fragment_news_row_share_date), timestamp);
        holder.itemTimestampTextView.setText(timestampText);

        // Set visibility of deleted and ignored indicators.
        final boolean isAuthorIgnored = _ignoreService.isIgnored(entity.getOwnerGuid());
        final boolean isItemIgnored = _ignoreService.isIgnored(entity.getRemoteId());
        holder.deletedIndicator.setVisibility(entity.getState() == NewsListItemState.Deleted ?
                View.VISIBLE :
                View.GONE);
        holder.ignoredIndicator.setVisibility(isAuthorIgnored || isItemIgnored ? View.VISIBLE : View.GONE);

        // Click listener for the row.
        holder.layout.setOnClickListener(view -> {

            // When clicked on, show the share item activity if the item is a queued item in
            // error state only. Otherwise, item details for the item if the item is
            // in Decrypted or Deleted states.

            if (entity.getLocalId() != null &&
                    entity.getState() == NewsListItemState.SubmissionError) {

                _shareItemService.showShareItemActivity(_activity, entity.getLocalId());

            } else if (entity.getState() == NewsListItemState.Decrypted ||
                    entity.getState() == NewsListItemState.Deleted) {

                Intent intent = new Intent(_activity, NewsItemDetailActivity.class);
                intent.putExtra(NewsItemDetailActivity.EXTRA_NEWS_ITEM, entity);
                view.getContext().startActivity(intent);
            }
        });

        // Long click listener for the row.
        holder.layout.setOnLongClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(holder.layout.getContext(), holder.popupMenuAnchor);
            popupMenu.inflate(R.menu.popup_news_item);

            // Set enablement and on click listener of popup menu items.

            // The delete item is enabled if the item is owned by me and if it isn't already
            // deleted.
            MenuItem deleteMenuItem = popupMenu.getMenu().findItem(R.id.action_delete_news_item);
            deleteMenuItem.setEnabled(ownershipInfo.getOwnedBy() == OwnedBy.Me &&
                    (entity.getState() == NewsListItemState.SubmissionError ||
                            entity.getState() == NewsListItemState.Decrypted));
            deleteMenuItem.setOnMenuItemClickListener(item -> {
                _confirmationDialogUtil.showConfirmationDialog(_activity.getSupportFragmentManager(),
                        R.string.dialog_confirm_title_delete_item,
                        R.string.dialog_confirm_text_delete_item,
                        "deleteItemConfirmation",
                        () -> {
                            if (entity.getRemoteId() != null) {
                                _shareItemService.deleteItemAsync(entity.getRemoteId(),
                                        _prefsUtil.getCurrentUserAccountId(),
                                        _prefsUtil.getCurrentHashedPassword());
                            } else if( entity.getLocalId() != null ) {
                                _shareItemService.deleteQueuedItem(entity.getLocalId());
                                _eventBus.post(new QueuedItemDeleted());
                            }
                        });
                return true;
            });

            // The add friend is enabled if the item is owned by a stranger.
            MenuItem addFriendMenuItem = popupMenu.getMenu().findItem(R.id.action_add_friend);
            addFriendMenuItem.setEnabled(ownershipInfo.getOwnedBy() == OwnedBy.Stranger);
            addFriendMenuItem.setOnMenuItemClickListener(item -> {
                // Post an event, let the fragment handle it.
                _eventBus.post(new AddFriendRequested(entity.getOwnerGuid()));
                return true;
            });

            // The ignore item is enabled if the item is not owned by me.
            MenuItem ignoreAuthor = popupMenu.getMenu().findItem(R.id.action_ignore_author);
            MenuItem ignoreItem = popupMenu.getMenu().findItem(R.id.action_ignore_item);
            ignoreAuthor.setEnabled(ownershipInfo.getOwnedBy() != OwnedBy.Me);
            ignoreItem.setEnabled(ownershipInfo.getOwnedBy() != OwnedBy.Me);

            // Set the ignore item text -- will be "ignore" or "unignore"
            if (ownershipInfo.getOwnedBy() != OwnedBy.Me) {
                if (isAuthorIgnored) {
                    ignoreAuthor.setTitle(_activity.getString(R.string.app_text_unignore_author));
                } else {
                    ignoreAuthor.setTitle(_activity.getString(R.string.app_text_ignore_author));
                }
                if (isItemIgnored) {
                    ignoreItem.setTitle(_activity.getString(R.string.app_text_unignore_item));
                } else {
                    ignoreItem.setTitle(_activity.getString(R.string.app_text_ignore_item));
                }
            }

            // Set up on click listeners for ignore/unignore items.
            ignoreAuthor.setOnMenuItemClickListener(item -> {
                if (isAuthorIgnored) {
                    _ignoreService.unignore(entity.getOwnerGuid());
                } else {
                    _ignoreService.ignore(entity.getOwnerGuid());
                }
                filter(true);
                return true;
            });
            ignoreItem.setOnMenuItemClickListener(item -> {
                if (isItemIgnored) {
                    _ignoreService.unignore(entity.getRemoteId());
                } else {
                    _ignoreService.ignore(entity.getRemoteId());
                }
                filter(true);
                return true;
            });

            popupMenu.show();
            return true;
        });
    }

    private void bindViewHolder(FooterViewHolder holder) {

        holder.layout.setSelected(false);
        holder.noMoreTextView.setVisibility(_remainingItemCount == 0 ? View.VISIBLE : View.GONE);
        holder.loadMoreTextView.setVisibility(_remainingItemCount > 0 ? View.VISIBLE : View.GONE);

        holder.loadMoreTextView.setText(String.format(_activity.getString(R.string.fragment_news_text_load_more_items),
                _remainingItemCount));

        // Click listener for the row. Posts an event to go fetch item headers that the NewsFragment
        // will pick up.
        if (_remainingItemCount > 0) {
            holder.layout.setOnClickListener(view -> {
                // Determine the low item rid in the model.
                long lowItemRid = Long.MAX_VALUE;
                for (AbstractNewsListItem entity : _allItems) {
                    if (entity.getRemoteId() != null) {
                        lowItemRid = Math.min(lowItemRid, entity.getRemoteId());
                    }
                }

                _eventBus.post(new FetchItemHeadersRequested(lowItemRid));
            });
        } else {
            holder.layout.setOnClickListener(null);
            holder.layout.setClickable(false);
        }
    }

    @Override
    public int getItemCount() {

        // We want to return the number of items plus one to show the footer, but only if the
        // number of filtered items is greater than zero or there are more items to fetch from the
        // server. Otherwise, just return the number of filtered items.
        if (_filteredItems.size() > 0 || _remainingItemCount > 0) {
            return _filteredItems.size() + 1;
        } else {
            return _filteredItems.size();
        }
    }

    /**
     * Update the state of an individual item in the list. Does not filter the list afterwards, you
     * will need to do that yourself.
     *
     * @param itemRid
     * @param state
     */
    public void updateState(long itemRid, NewsListItemState state) {
        for (AbstractNewsListItem item : _allItems) {
            if (item.getRemoteId() != null &&
                    item.getRemoteId().compareTo(itemRid) == 0) {
                item.setState(state);
                break;
            }
        }
    }

    /**
     * Merges a list of items into the list. Does not filter the list afterwards.
     *
     * @param items
     */
    public void merge(List<AbstractNewsListItem> items) {
        for (AbstractNewsListItem item : items) {
            merge(item);
        }
    }

    /**
     * Merges a single item into the list. Does not filter the list afterwards.
     *
     * @param item
     */
    public void merge(AbstractNewsListItem item) {

        remove(_allItems, item);

        // If this is a response, just return after we removed it from the model. Kinda gross.
        if (item instanceof ResponseNewsListItem) {
            return;
        }

        // Add the item to the model.
        _allItems.add(item);
    }

    /**
     * Removes a single item from a list of items, based on the item's remote or local id.
     *
     * @param itemList
     * @param item
     */
    private void remove(List<AbstractNewsListItem> itemList, AbstractNewsListItem item) {

        // If the news item already exists in the model, remove it before adding the new one.
        // Also possible to update the existing one from the new one, but this is easier for now.
        // This is complicated somewhat by the fact that we're merging data from two different
        // sources - the shares from the server and the queued items from the database on the
        // local device.

        AbstractNewsListItem itemToRemove = null;
        for (AbstractNewsListItem existingItem : itemList) {
            if (existingItem.getRemoteId() != null &&
                    item.getRemoteId() != null &&
                    existingItem.getRemoteId().compareTo(item.getRemoteId()) == 0) {
                itemToRemove = existingItem;
                break;
            }
            if (existingItem.getLocalId() != null &&
                    item.getLocalId() != null &&
                    existingItem.getLocalId().compareTo(item.getLocalId()) == 0) {
                itemToRemove = existingItem;
                break;
            }
        }
        if (itemToRemove != null) {
            itemList.remove(itemToRemove);
        }
    }

    public List<AbstractNewsListItem> getModel() {
        return _allItems;
    }

    public void filter(boolean notifyDataSetChanged) {

        // Remove everything from the filtered items.
        _filteredItems.clear();

        // Go through the items in the "all items" list, adding each to the model as appropriate.
        // As we go, increment the filter counts so we can update the user interface.

        for (AbstractNewsListItem item : _allItems) {

            EnumSet<FilterType> filters = _prefsUtil.getFilterTypes();

            // Go get the ownership info and set deleted and ignored flags for use below.
            OwnershipInfo ownershipInfo = _ownershipService.getOwnership(item);
            boolean isDeleted = item.getState() == NewsListItemState.Deleted;
            boolean isIgnored = _ignoreService.isIgnored(item.getOwnerGuid(), item.getRemoteId());

            boolean noPersonFilters = true;

            // Do some filtering based on who owns the item.
            boolean includeBasedOnPerson = false;
            if (filters.contains(FilterType.Mine)) {
                includeBasedOnPerson = ownershipInfo.getOwnedBy() == OwnedBy.Me;
                noPersonFilters = false;
            }
            if (!includeBasedOnPerson && filters.contains(FilterType.Friends)) {
                includeBasedOnPerson = ownershipInfo.getOwnedBy() == OwnedBy.Friend;
                noPersonFilters = false;
            }
            if (!includeBasedOnPerson && filters.contains(FilterType.Strangers)) {
                includeBasedOnPerson = ownershipInfo.getOwnedBy() == OwnedBy.Stranger;
                noPersonFilters = false;
            }

            // Filter based on excluding by status (deleted or ignored).
            boolean excludeBasedOnStatus = false;
            if (!filters.contains(FilterType.Deleted)) {
                excludeBasedOnStatus = isDeleted;
            }
            if (!excludeBasedOnStatus && !filters.contains(FilterType.Ignored)) {
                excludeBasedOnStatus = isIgnored;
            }

            // Filter based on including by status (deleted or ignored).
            boolean includeBasedOnStatus = false;
            if (filters.contains(FilterType.Deleted)) {
                includeBasedOnStatus = isDeleted;
            }
            if (!includeBasedOnStatus && filters.contains(FilterType.Ignored)) {
                includeBasedOnStatus = isIgnored;
            }

            // Decide whether or not to add the item. We will add the item if we are filtering for
            // the person who owns the item and we're not excluding based on status. We also include
            // the item in the list if we have no ownership filters, in which case we include based
            // on the status filter (deleted or ignored).
            if (includeBasedOnPerson && !excludeBasedOnStatus) {
                _filteredItems.add(item);
            } else if (noPersonFilters && includeBasedOnStatus) {
                _filteredItems.add(item);
            }
        }

        // Sort the model.
        Collections.sort(_filteredItems, (lhs, rhs) -> Long.compare(rhs.getItemTimestamp(), lhs.getItemTimestamp()));

        // Notify.
        if (notifyDataSetChanged) {
            notifyDataSetChanged();
        }

        _eventBus.post(new NewsFilterApplied());
    }

    public boolean isListEmpty() {
        return _filteredItems.isEmpty();
    }

    public void updateRemainingItemCount(long remainingItemCount) {
        _remainingItemCount = remainingItemCount;
        notifyDataSetChanged();
    }

    public long getRemainingItemCount() {
        return _remainingItemCount;
    }

    public void truncateModel() {
        _filteredItems.clear();
        _allItems.clear();
        _remainingItemCount = -1L;
        notifyDataSetChanged();
    }

    public void removeLocalItems() {
        List<AbstractNewsListItem> itemsToRemove = new ArrayList<>();
        for (AbstractNewsListItem item : _allItems) {
            if (item.getLocalId() != null) {
                itemsToRemove.add(item);
            }
        }
        _allItems.removeAll(itemsToRemove);
    }

    public class RegularViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        FrameLayout avatarCircleLayout;
        ImageView avatarCircleImageView;
        TextView avatarCircleTextView;
        TextView decryptionFailedTextView;
        TextView deserializationFailedTextView;
        TextView decryptingTextView;
        TextView itemTitleTextView;
        TextView itemTimestampTextView;
        ImageView thumbnailImageView;
        TextView statusTextView;
        FrameLayout popupMenuAnchor;
        ImageView deletedIndicator;
        ImageView ignoredIndicator;

        RegularViewHolder(View v) {
            super(v);
            layout = v;
            avatarCircleLayout = v.findViewById(R.id.frame_layout_avatar_circle);
            avatarCircleImageView = v.findViewById(R.id.image_view_avatar_circle);
            avatarCircleTextView = v.findViewById(R.id.text_view_avatar_circle);
            decryptionFailedTextView = v.findViewById(R.id.text_view_decryption_failed);
            deserializationFailedTextView = v.findViewById(R.id.text_view_deserialization_failed);
            decryptingTextView = v.findViewById(R.id.text_view_decrypting);
            itemTitleTextView = v.findViewById(R.id.text_view_news_item_title);
            itemTimestampTextView = v.findViewById(R.id.text_view_news_item_timestamp);
            thumbnailImageView = v.findViewById(R.id.image_view_news_item_thumbnail);
            statusTextView = v.findViewById(R.id.text_view_news_item_status);
            popupMenuAnchor = v.findViewById(R.id.popup_menu_anchor);
            deletedIndicator = v.findViewById(R.id.image_view_indicator_deleted);
            ignoredIndicator = v.findViewById(R.id.image_view_indicator_ignored);
        }
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        TextView loadMoreTextView;
        TextView noMoreTextView;

        FooterViewHolder(View v) {
            super(v);
            layout = v;
            loadMoreTextView = v.findViewById(R.id.button_load_more_news);
            noMoreTextView = v.findViewById(R.id.text_no_more_news);
        }
    }
}
