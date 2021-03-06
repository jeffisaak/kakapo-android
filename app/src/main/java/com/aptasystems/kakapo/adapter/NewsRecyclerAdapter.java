package com.aptasystems.kakapo.adapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.aptasystems.kakapo.dao.UserAccountDAO;
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
import com.aptasystems.kakapo.service.UserAccountService;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;
import com.aptasystems.kakapo.util.FilterType;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.util.TimePresentationUtil;
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
import kakapo.util.TimeUtil;

public class NewsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @Inject
    ShareService _shareItemService;

    @Inject
    EventBus _eventBus;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    FriendService _friendService;

    @Inject
    IgnoreService _ignoreService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    UserAccountService _userAccountService;

    @Inject
    OwnershipService _ownershipService;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    @Inject
    TimePresentationUtil _timePresentationUtil;

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
        holder.colourCodeLayout.setBackgroundColor(ownershipInfo.getColour());

        // Set all our text views gone and then based on the state, show some things.
        holder.decryptionFailed.setVisibility(View.GONE);
        holder.deserializationFailed.setVisibility(View.GONE);
        holder.decrypting.setVisibility(View.GONE);
        holder.itemTitle.setVisibility(View.GONE);
        holder.itemStatus.setVisibility(View.GONE);
        holder.errorLayout.setVisibility(View.GONE);
        switch (entity.getState()) {
            case Decrypting:
                holder.decrypting.setVisibility(View.VISIBLE);
                break;
            case Decrypted:
            case Deleted:
                holder.itemTitle.setVisibility(View.VISIBLE);
                break;
            case Queued:
                holder.itemTitle.setVisibility(View.VISIBLE);
                holder.itemStatus.setVisibility(View.VISIBLE);
                holder.itemStatus.setText(_activity.getString(R.string.app_text_queued_item));
                break;
            case Submitting:
                holder.itemTitle.setVisibility(View.VISIBLE);
                holder.itemStatus.setVisibility(View.VISIBLE);
                holder.itemStatus.setText(_activity.getString(R.string.fragment_news_encrypting_and_uploading));
                break;
            case SubmissionError:
                holder.itemTitle.setVisibility(View.VISIBLE);
                holder.errorLayout.setVisibility(View.VISIBLE);
                break;
            case DecryptionFailed:
                holder.decryptionFailed.setVisibility(View.VISIBLE);
                break;
            case DeserializationFailed:
                holder.deserializationFailed.setVisibility(View.VISIBLE);
                break;
        }

        RegularNewsListItem newsListItem = (RegularNewsListItem) entity;

        // Set the title.
        if (newsListItem.getState() == NewsListItemState.Deleted) {
            holder.itemTitle.setText(_activity.getString(R.string.app_text_deleted_item));
        } else if (newsListItem.getTitle() != null) {
            holder.itemTitle.setText(newsListItem.getTitle());
        }

        if (newsListItem.getThumbnailData() != null) {

            // If we have a thumbnail, create a bitmap and show it.
            Bitmap thumbnail = BitmapFactory.decodeByteArray(newsListItem.getThumbnailData(),
                    0,
                    newsListItem.getThumbnailData().length);
            holder.thumbnailImage.setImageBitmap(thumbnail);
            holder.thumbnailImage.setVisibility(View.VISIBLE);

            holder.thumbnailImage.setOnClickListener(new DoubleClickPreventingOnClickListener() {

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
            holder.thumbnailImage.setImageBitmap(null);
            holder.thumbnailImage.setVisibility(View.GONE);
            holder.thumbnailImage.setOnClickListener(null);
            holder.thumbnailImage.setClickable(false);
        }

        // Convert the item's timestamp (which is in GMT) to local time and format it for display.
        long timestampInZulu = TimeUtil.timestampInZulu(entity.getItemTimestamp());
        String timestampText = _timePresentationUtil.formatAsTimePast(timestampInZulu);
        holder.itemTimestamp.setText(timestampText);
        holder.ownerText.setText(ownershipInfo.getReference(true) + " (" + entity.getOwnerGuid() + ")");
        holder.responseCount.setText(String.format("%1$d", entity.getChildCount()));

        // Click listener for the row. We'll set it on three different views.
        View.OnClickListener rowClickListener = v -> {
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
                v.getContext().startActivity(intent);
            }
        };
        holder.titleLayout.setOnClickListener(rowClickListener);
        holder.metadataLayout.setOnClickListener(rowClickListener);
        holder.errorLayout.setOnClickListener(rowClickListener);

        // Long click listener for the row.
        View.OnLongClickListener rowLongClickListener = v -> {
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
                                        _prefsUtil.getCurrentPassword());
                            } else if (entity.getLocalId() != null) {
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
            final boolean isAuthorIgnored = _ignoreService.isIgnored(entity.getOwnerGuid());
            final boolean isItemIgnored = _ignoreService.isIgnored(entity.getRemoteId());
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

            // The blacklist item is enabled if the item is not owned by me.
            MenuItem blacklistAuthor = popupMenu.getMenu().findItem(R.id.action_blacklist_author);
            blacklistAuthor.setEnabled(ownershipInfo.getOwnedBy() != OwnedBy.Me);
            blacklistAuthor.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    _confirmationDialogUtil.showConfirmationDialog(_activity.getSupportFragmentManager(),
                            R.string.dialog_confirm_title_blacklist_author,
                            R.string.dialog_confirm_text_blacklist_author,
                            "blacklistAuthorConfirmation",
                            () -> {
                                _userAccountService.blacklistAuthorAsync(_prefsUtil.getCurrentUserAccountId(),
                                        _prefsUtil.getCurrentPassword(),
                                        entity.getOwnerGuid());
                            });

                    return true;
                }
            });

            popupMenu.show();
            return true;            };
        holder.titleLayout.setOnLongClickListener(rowLongClickListener);
        holder.metadataLayout.setOnLongClickListener(rowLongClickListener);
        holder.errorLayout.setOnLongClickListener(rowLongClickListener);
    }

    private void bindViewHolder(FooterViewHolder holder) {

        holder.layout.setSelected(false);
        holder.noMoreNews.setVisibility(_remainingItemCount == 0 ? View.VISIBLE : View.GONE);
        holder.loadMoreButton.setVisibility(_remainingItemCount > 0 ? View.VISIBLE : View.GONE);

        holder.loadMoreButton.setText(String.format(_activity.getString(R.string.fragment_news_text_load_more_items),
                _remainingItemCount));

        // Click listener for the row. Posts an event to go fetch item headers that the NewsFragment
        // will pick up.
        if (_remainingItemCount > 0) {
            holder.layout.setOnClickListener(view -> {
                // Determine the low item remote id in the model.
                long lowItemRemoteId = Long.MAX_VALUE;
                for (AbstractNewsListItem entity : _allItems) {
                    if (entity.getRemoteId() != null) {
                        lowItemRemoteId = Math.min(lowItemRemoteId, entity.getRemoteId());
                    }
                }

                _eventBus.post(new FetchItemHeadersRequested(lowItemRemoteId));
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
     * @param itemRemoteId
     * @param state
     */
    public void updateState(long itemRemoteId, NewsListItemState state) {
        for (AbstractNewsListItem item : _allItems) {
            if (item.getRemoteId() != null &&
                    item.getRemoteId().compareTo(itemRemoteId) == 0) {
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

    public static class RegularViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        FrameLayout colourCodeLayout;
        View titleLayout;
        View metadataLayout;
        TextView decryptionFailed;
        TextView deserializationFailed;
        TextView decrypting;
        TextView itemTitle;
        TextView itemTimestamp;
        TextView responseCount;
        TextView ownerText;
        ImageView thumbnailImage;
        View errorLayout;
        TextView itemStatus;
        FrameLayout popupMenuAnchor;

        RegularViewHolder(View v) {
            super(v);
            layout = v;
            colourCodeLayout = v.findViewById(R.id.colour_code_layout);
            titleLayout = v.findViewById(R.id.title_layout);
            metadataLayout = v.findViewById(R.id.metadata_layout);
            decryptionFailed = v.findViewById(R.id.decryption_failed);
            deserializationFailed = v.findViewById(R.id.deserialization_failed);
            decrypting = v.findViewById(R.id.decrypting);
            itemTitle = v.findViewById(R.id.item_title);
            itemTimestamp = v.findViewById(R.id.item_timestamp);
            responseCount = v.findViewById(R.id.response_count);
            ownerText = v.findViewById(R.id.owner_text);
            thumbnailImage = v.findViewById(R.id.thumbnail_image);
            errorLayout = v.findViewById(R.id.error_layout);
            itemStatus = v.findViewById(R.id.status_text);
            popupMenuAnchor = v.findViewById(R.id.popup_menu_anchor);
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        TextView loadMoreButton;
        TextView noMoreNews;

        FooterViewHolder(View v) {
            super(v);
            layout = v;
            loadMoreButton = v.findViewById(R.id.load_more_button);
            noMoreNews = v.findViewById(R.id.no_more_news);
        }
    }
}
