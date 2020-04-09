package com.aptasystems.kakapo.adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.ViewImageActivity;
import com.aptasystems.kakapo.adapter.model.NewsListItemState;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.service.FriendService;
import com.aptasystems.kakapo.service.IgnoreService;
import com.aptasystems.kakapo.service.OwnedBy;
import com.aptasystems.kakapo.service.OwnershipInfo;
import com.aptasystems.kakapo.service.OwnershipService;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.event.AddFriendRequested;
import com.aptasystems.kakapo.event.HideResponseLayout;
import com.aptasystems.kakapo.event.ShowResponseLayout;
import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.adapter.model.RegularNewsListItem;
import com.aptasystems.kakapo.adapter.model.ResponseNewsListItem;
import com.aptasystems.kakapo.service.UserAccountService;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.view.DoubleClickPreventingOnClickListener;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

public class NewsDetailRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = NewsDetailRecyclerAdapter.class.getSimpleName();

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    ShareService _shareItemService;

    @Inject
    EventBus _eventBus;

    @Inject
    FriendService _friendService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    OwnershipService _ownershipService;

    @Inject
    IgnoreService _ignoreService;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    @Inject
    UserAccountService _userAccountService;

    private FragmentActivity _activity;
    private List<AbstractNewsListItem> _model;

    private static final int VIEW_TYPE_REGULAR = 0;
    private static final int VIEW_TYPE_RESPONSE = 1;

    public NewsDetailRecyclerAdapter(FragmentActivity activity) {
        _activity = activity;
        _model = new ArrayList<>();
        ((KakapoApplication) activity.getApplicationContext()).getKakapoComponent().inject(this);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_REGULAR:
                return new RegularItemViewHolder(inflater.inflate(R.layout.row_news_detail, parent, false));
            case VIEW_TYPE_RESPONSE:
                return new ResponseViewHolder(inflater.inflate(R.layout.row_news_detail_response, parent, false));
        }
        // Shouldn't actually get here.
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        final AbstractNewsListItem entity = _model.get(position);
        if (entity instanceof RegularNewsListItem) {
            return VIEW_TYPE_REGULAR;
        } else if (entity instanceof ResponseNewsListItem) {
            return VIEW_TYPE_RESPONSE;
        }
        // Shouldn't actually get here.
        return -1;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final AbstractNewsListItem entity = _model.get(position);
        if (entity instanceof RegularNewsListItem) {
            bindViewHolder((RegularItemViewHolder) holder, (RegularNewsListItem) entity);
        } else if (entity instanceof ResponseNewsListItem) {
            bindViewHolder((ResponseViewHolder) holder, (ResponseNewsListItem) entity, position);
        }
    }

    private void bindViewHolder(RegularItemViewHolder holder, RegularNewsListItem entity) {

        OwnershipInfo ownershipInfo = _ownershipService.getOwnership(entity);

        // Title.
        if (entity.getState() == NewsListItemState.Blacklisted) {
            holder.itemTitle.setText("[blacklisted]");
        } else if (entity.getState() == NewsListItemState.Deleted) {
            holder.itemTitle.setText(_activity.getString(R.string.app_text_deleted_item));
        } else {
            holder.itemTitle.setText(entity.getTitle());
        }

        String ownerName = ownershipInfo.getReference(false);

        // "Shared by..." text.
        String timestamp = DateUtils.formatDateTime(_activity,
                entity.getItemTimestamp(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME);
        String sharedByText = String.format(_activity.getString(R.string.item_detail_text_shared_by), ownerName, timestamp);
        holder.sharedBy.setText(sharedByText);

        // Thumbnail (optional).
        if (entity.getThumbnailData() != null) {
            Bitmap thumbnail = BitmapFactory.decodeByteArray(entity.getThumbnailData(), 0, entity.getThumbnailData().length);
            holder.thumbnailImage.setImageBitmap(thumbnail);
            holder.thumbnailImage.setVisibility(View.VISIBLE);

            holder.thumbnailImage.setOnClickListener(new DoubleClickPreventingOnClickListener() {
                @Override
                public void onClickInternal(View view) {
                    if (entity.getState() == NewsListItemState.Decrypted) {
                        // Open the view image activity.
                        Intent intent = new Intent(_activity, ViewImageActivity.class);
                        intent.putExtra(ViewImageActivity.EXTRA_ITEM_REMOTE_ID, entity.getRemoteId());
                        view.getContext().startActivity(intent);
                    }
                }
            });

        } else {
            holder.thumbnailImage.setImageBitmap(null);
            holder.thumbnailImage.setVisibility(View.GONE);
            holder.thumbnailImage.setOnClickListener(null);
            holder.thumbnailImage.setClickable(false);
        }

        // URL (optional).
        if (entity.getUrl() != null) {
            holder.urlLayout.setVisibility(View.VISIBLE);
            holder.urlText.setText(entity.getUrl());
            holder.urlButton.setOnClickListener(v -> {

                // First check that we have a well-formed URL that can be handled by the view activity.
                String url = URLUtil.guessUrl(entity.getUrl());

                if (URLUtil.isHttpUrl(url) ||
                        URLUtil.isHttpsUrl(url)) {

                    _confirmationDialogUtil.showConfirmationDialog(_activity.getSupportFragmentManager(),
                            R.string.dialog_confirm_title_follow_link,
                            R.string.dialog_confirm_text_follow_link,
                            "followLinkConfirmation",
                            () -> {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                _activity.startActivity(browserIntent);
                            });
                } else {
                    // Badly formed or non-http/https URL.
                    Snackbar.make(v, "This does not appear to be a valid URL.", Snackbar.LENGTH_SHORT)
                            .show();
                }

            });
        } else {
            holder.urlLayout.setVisibility(View.GONE);
            holder.urlButton.setOnClickListener(null);
        }

        // Message (optional).
        if (entity.getMessage() != null) {
            holder.messageText.setText(entity.getMessage());
            holder.messageText.setVisibility(View.VISIBLE);
        } else {
            holder.messageText.setVisibility(View.GONE);
        }
    }

    private void bindViewHolder(ResponseViewHolder holder, ResponseNewsListItem entity, int position) {

        // Gather some info about the entity.
        OwnershipInfo ownershipInfo = _ownershipService.getOwnership(entity);
        final boolean isAuthorIgnored = _ignoreService.isIgnored(entity.getOwnerGuid());
        final boolean isItemIgnored = _ignoreService.isIgnored(entity.getRemoteId());

        // Set all our text views gone and then based on the state, show some things.
        holder.decryptionFailed.setVisibility(View.GONE);
        holder.deserializationFailed.setVisibility(View.GONE);
        holder.decrypting.setVisibility(View.GONE);
        holder.statusText.setVisibility(View.GONE);
        holder.messageText.setVisibility(View.GONE);
        switch (entity.getState()) {
            case Decrypting:
                holder.decrypting.setVisibility(View.VISIBLE);
                break;
            case Decrypted:
            case Deleted:
            case Blacklisted:
                holder.messageText.setVisibility(View.VISIBLE);
                break;
            case Queued:
                holder.messageText.setVisibility(View.VISIBLE);
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(_activity.getString(R.string.item_detail_queued_item));
                break;
            case Submitting:
                holder.messageText.setVisibility(View.VISIBLE);
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(_activity.getString(R.string.fragment_news_encrypting_and_uploading));
                break;
            case SubmissionError:
                holder.messageText.setVisibility(View.VISIBLE);
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(_activity.getString(R.string.fragment_news_upload_failed));
                break;
            case DecryptionFailed:
                holder.decryptionFailed.setVisibility(View.VISIBLE);
                break;
            case DeserializationFailed:
                holder.deserializationFailed.setVisibility(View.VISIBLE);
                break;
        }

        // Figure out the amount to indent. Start at -1 because first indenting level should
        // be zero, not one.
        int indentLevel = -1;
        AbstractNewsListItem response = entity;
        while (response.getParentItemRemoteId() != null) {
            for (AbstractNewsListItem testItem : _model) {
                if (testItem.getRemoteId() != null && testItem.getRemoteId().compareTo(response.getParentItemRemoteId()) == 0) {
                    indentLevel++;
                    response = testItem;
                    break;
                }
            }

            // We've gone through the items and couldn't find the parent. This happens when we're
            // showing the user some queued responses. The responses have parents, but the parents
            // may not yet be downloaded from the server. In this case we just want to return the
            // original item. Kludgy.
            if (response == entity) {
                break;
            }
        }

        // Perform indenting.
        int indentInPixels = holder.layout.getContext().getResources().getDimensionPixelSize(R.dimen.reply_indent);
        holder.layout.setPadding(
                indentInPixels * indentLevel,
                holder.layout.getPaddingTop(),
                holder.layout.getPaddingRight(),
                holder.layout.getPaddingBottom());

        // Set the colour.
        holder.colourCodeLayout.setBackgroundColor(ownershipInfo.getColour());

        // "Shared by and when" text.
        String timestamp = DateUtils.formatDateTime(_activity,
                entity.getItemTimestamp(),
                DateUtils.FORMAT_ABBREV_ALL |
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME);
        String ownerAndTimeText = String.format(_activity.getString(R.string.item_detail_text_response_shared_by), ownershipInfo.getReference(false), timestamp);
        holder.sharedBy.setText(ownerAndTimeText);

        // Set the message.
        if (entity.getState() == NewsListItemState.Blacklisted) {
            holder.messageText.setText("[blacklisted]");
        } else if (entity.getState() == NewsListItemState.Deleted) {
            holder.messageText.setText(_activity.getString(R.string.app_text_deleted_response));
        } else if (isAuthorIgnored || isItemIgnored) {
            holder.messageText.setText(_activity.getString(R.string.app_text_ignored_response));
        } else {
            holder.messageText.setText(entity.getMessage());
        }

        // Click listener for the row.
        holder.layout.setOnClickListener(new DoubleClickPreventingOnClickListener() {

            @Override
            public void onClickInternal(View view) {

                // When clicked on, show the share item activity if the item is a queued item in
                // error state only. Otherwise, show the button bar for the item if the item is
                // in Decrypted state.

                if (entity.getLocalId() != null &&
                        entity.getState() == NewsListItemState.SubmissionError) {

                    _shareItemService.showShareItemActivity(_activity, entity.getLocalId());

                } else if (entity.getState() == NewsListItemState.Decrypted ||
                        entity.getState() == NewsListItemState.DecryptionFailed ||
                        entity.getState() == NewsListItemState.DeserializationFailed ||
                        entity.getState() == NewsListItemState.Deleted) {

                    // If the item is ignored, a first tap will reveal the item. Kludgy, but we're
                    // going to check the text to see if it's ignored.
                    if (holder.messageText.getText().toString().compareTo(_activity.getString(R.string.app_text_ignored_response)) == 0) {
                        holder.messageText.setText(entity.getMessage());
                    } else {
                        _eventBus.post(new ShowResponseLayout(position, entity.getRemoteId()));
                    }
                }

            }
        });

        holder.layout.setOnLongClickListener(v -> {

            _eventBus.post(new HideResponseLayout());

            if (entity.getState() == NewsListItemState.Decrypted) {
                PopupMenu popupMenu = new PopupMenu(holder.layout.getContext(), holder.popupMenuAnchor);
                popupMenu.inflate(R.menu.popup_news_item);

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
                                _shareItemService.deleteItemAsync(entity.getRemoteId(),
                                        _prefsUtil.getCurrentUserAccountId(),
                                        _prefsUtil.getCurrentPassword());
                            });

                    return true;
                });

                // The add friend is enabled if the item is owned by a stranger.
                MenuItem addFriendMenuItem = popupMenu.getMenu().findItem(R.id.action_add_friend);
                addFriendMenuItem.setEnabled(ownershipInfo.getOwnedBy() == OwnedBy.Stranger);
                addFriendMenuItem.setOnMenuItemClickListener(item -> {
                    // Post an event, let the activity handle it.
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
                        ignoreItem.setTitle(_activity.getString(R.string.app_text_unignore_reply));
                    } else {
                        ignoreItem.setTitle(_activity.getString(R.string.app_text_ignore_reply));
                    }
                }

                // Set up on click listeners for ignore/unignore items.
                ignoreAuthor.setOnMenuItemClickListener(item -> {
                    if (isAuthorIgnored) {
                        _ignoreService.unignore(entity.getOwnerGuid());
                    } else {
                        _ignoreService.ignore(entity.getOwnerGuid());
                    }
                    notifyDataSetChanged();
                    return true;
                });
                ignoreItem.setOnMenuItemClickListener(item -> {
                    if (isItemIgnored) {
                        _ignoreService.unignore(entity.getRemoteId());
                    } else {
                        _ignoreService.ignore(entity.getRemoteId());
                    }
                    notifyDataSetChanged();
                    return true;
                });

                // The blacklist item is enabled if the item is not owned by me and not already
                // blacklisted.
                MenuItem blacklistAuthor = popupMenu.getMenu().findItem(R.id.action_blacklist_author);
                blacklistAuthor.setEnabled(ownershipInfo.getOwnedBy() != OwnedBy.Me &&
                        entity.getState() != NewsListItemState.Blacklisted);
                blacklistAuthor.setOnMenuItemClickListener(item -> {

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
                });

                popupMenu.show();
                return true;
            } else {
                return false;
            }
        });

    }

    @Override
    public int getItemCount() {
        return _model.size();
    }

    public AbstractNewsListItem getItem(int index) {
        return _model.get(index);
    }

    public void updateState(long itemRemoteId, NewsListItemState state) {
        for (AbstractNewsListItem item : _model) {
            if (item.getRemoteId() != null &&
                    item.getRemoteId().compareTo(itemRemoteId) == 0) {
                item.setState(state);
                break;
            }
        }
        sortModel();
        notifyDataSetChanged();
    }

    public void merge(Collection<AbstractNewsListItem> items) {
        for (AbstractNewsListItem item : items) {
            merge(item, false);
        }
        sortModel();
        notifyDataSetChanged();
    }

    public void merge(AbstractNewsListItem item, boolean sortAndNotify) {

        // If the news item already exists in the model, remove it before adding the new one.
        // Also possible to update the existing one from the new one, but this is easier for now.
        // This is complicated somewhat by the fact that we're merging data from two different
        // sources - the shares from the server and the queued items from the database on the
        // local device.
        AbstractNewsListItem itemToRemove = null;
        for (AbstractNewsListItem existingItem : _model) {
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
            _model.remove(itemToRemove);
        }

        // Add the item to the model.
        _model.add(item);

        if (sortAndNotify) {
            sortModel();
            notifyDataSetChanged();
        }
    }

    private void sortModel() {

        // Here we need to sort the model so that a) the original item stays at the top and b) the
        // tree of responses is presented nicely.

        // This works, but I don't know how performant it will be with large datasets.
        Collections.sort(_model, new Comparator<AbstractNewsListItem>() {

            private LongSparseArray<AbstractNewsListItem> _newsArray;
            @Override
            public int compare(AbstractNewsListItem lhs, AbstractNewsListItem rhs) {

                // Build a sparse long array of ids and news list items.
                // This will provide a bit of a performance improvement later.
                _newsArray = new LongSparseArray<>();
                for (AbstractNewsListItem newsItem : _model) {
                    if (newsItem.getRemoteId() != null) {
                        _newsArray.put(newsItem.getRemoteId(), newsItem);
                    }
                }

                StringBuilder lhsSortValueStringBuilder = new StringBuilder();
                if (lhs.getResponseSortValue() == null) {
                    lhs.setResponseSortValue(getSortValue(lhs));
                }
                lhsSortValueStringBuilder.append(lhs.getResponseSortValue());

                StringBuilder rhsSortValueStringBuilder = new StringBuilder();
                if (rhs.getResponseSortValue() == null) {
                    rhs.setResponseSortValue(getSortValue(rhs));
                }
                rhsSortValueStringBuilder.append(rhs.getResponseSortValue());

                // Now we have the two sort values, but they may be different length.
                // Zero right pad until they are the same length.
                while (lhsSortValueStringBuilder.length() < rhsSortValueStringBuilder.length()) {
                    lhsSortValueStringBuilder.append("0");
                }
                while (rhsSortValueStringBuilder.length() < lhsSortValueStringBuilder.length()) {
                    rhsSortValueStringBuilder.append("0");
                }

                // Compare!
                return lhsSortValueStringBuilder.toString().compareTo(rhsSortValueStringBuilder.toString());
            }

            @SuppressLint("DefaultLocale")
            private String getSortValue(AbstractNewsListItem item) {
                StringBuilder result = new StringBuilder();

                // If this item has a parent, go find it and call this method with the parent.
                if (item.getParentItemRemoteId() != null) {
                    result.append(getSortValue(_newsArray.get(item.getParentItemRemoteId())));
                }

                // Append this item's rid and return the string.
                if (item.getLocalId() != null) {
                    result.append(String.format("9%019d", item.getLocalId()));
                } else {
                    result.append(String.format("%020d", item.getRemoteId()));
                }

                return result.toString();
            }
        });
    }

    public void removeLocalItems() {
        List<AbstractNewsListItem> itemsToRemove = new ArrayList<>();
        for (AbstractNewsListItem item : _model) {
            if (item.getLocalId() != null) {
                itemsToRemove.add(item);
            }
        }
        _model.removeAll(itemsToRemove);
    }

    public void clearModel() {
        _model.clear();
    }

    public class RegularItemViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        public TextView itemTitle;
        public TextView sharedBy;
        public ImageView thumbnailImage;
        public View urlLayout;
        public ImageButton urlButton;
        public TextView urlText;
        public TextView messageText;

        public RegularItemViewHolder(View v) {
            super(v);
            layout = v;
            itemTitle = v.findViewById(R.id.item_title);
            sharedBy = v.findViewById(R.id.shared_by);
            thumbnailImage = v.findViewById(R.id.thumbnail_image);
            urlLayout = v.findViewById(R.id.url_layout);
            urlButton = v.findViewById(R.id.url_button);
            urlText = v.findViewById(R.id.url_text);
            messageText = v.findViewById(R.id.message_text);
        }
    }

    public class ResponseViewHolder extends RecyclerView.ViewHolder {
        public View layout;
        public FrameLayout colourCodeLayout;
        public TextView decryptionFailed;
        public TextView deserializationFailed;
        public TextView decrypting;
        public TextView messageText;
        public TextView sharedBy;
        public TextView statusText;
        public FrameLayout popupMenuAnchor;

        public ResponseViewHolder(View v) {
            super(v);
            layout = v;
            colourCodeLayout = v.findViewById(R.id.colour_code_layout);
            decryptionFailed = v.findViewById(R.id.decryption_failed);
            deserializationFailed = v.findViewById(R.id.deserialization_failed);
            decrypting = v.findViewById(R.id.decrypting);
            messageText = v.findViewById(R.id.message_text);
            sharedBy = v.findViewById(R.id.shared_by);
            statusText = v.findViewById(R.id.status_text);
            popupMenuAnchor = v.findViewById(R.id.popup_menu_anchor);
        }
    }
}
