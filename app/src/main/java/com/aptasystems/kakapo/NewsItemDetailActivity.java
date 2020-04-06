package com.aptasystems.kakapo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.aptasystems.kakapo.adapter.NewsDetailRecyclerAdapter;
import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.adapter.model.NewsListItemState;
import com.aptasystems.kakapo.adapter.model.RegularNewsListItem;
import com.aptasystems.kakapo.adapter.model.ResponseNewsListItem;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.ShareDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.databinding.ActivityNewsItemDetailBinding;
import com.aptasystems.kakapo.dialog.AddFriendDialog;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Share;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.event.AddFriendComplete;
import com.aptasystems.kakapo.event.BlacklistAuthorComplete;
import com.aptasystems.kakapo.event.DeleteItemComplete;
import com.aptasystems.kakapo.event.FetchItemHeadersComplete;
import com.aptasystems.kakapo.event.HideResponseLayout;
import com.aptasystems.kakapo.event.IgnoresChanged;
import com.aptasystems.kakapo.event.NewsItemDecryptComplete;
import com.aptasystems.kakapo.event.ScrollToResponse;
import com.aptasystems.kakapo.event.ShowResponseLayout;
import com.aptasystems.kakapo.event.SubmitItemComplete;
import com.aptasystems.kakapo.event.SubmitItemStarted;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.service.IgnoreService;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.service.UserAccountService;
import com.aptasystems.kakapo.util.ConfirmationDialogUtil;
import com.aptasystems.kakapo.util.KeyboardUtil;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import kakapo.api.model.ShareItem;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class NewsItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NEWS_ITEM = "newsItem";

    private static final String TAG = NewsItemDetailActivity.class.getSimpleName();
    private static final String SHOWCASE_ID = SelectUserAccountActivity.class.getSimpleName();

    @Inject
    ShareDAO _shareDAO;

    @Inject
    FriendDAO _friendDAO;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    EventBus _eventBus;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    ShareService _shareItemService;

    @Inject
    IgnoreService _ignoreService;

    @Inject
    ConfirmationDialogUtil _confirmationDialogUtil;

    @Inject
    UserAccountService _userAccountService;

    private NewsDetailRecyclerAdapter _recyclerViewAdapter;
    private Long _selectedItemRemoteId;
    private CompositeDisposable _compositeDisposable = new CompositeDisposable();
    private ActivityNewsItemDetailBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        _binding = ActivityNewsItemDetailBinding.inflate(getLayoutInflater());

        setContentView(_binding.getRoot());

        setSupportActionBar(_binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RegularNewsListItem newsItem = (RegularNewsListItem) getIntent().getSerializableExtra(EXTRA_NEWS_ITEM);

        // Set the title in the action bar.
        if (newsItem.getState() == NewsListItemState.Deleted) {
            setTitle("[deleted]");
        } else {
            setTitle(newsItem.getTitle());
        }

        // Set up the recycler view.
        _binding.includes.newsDetailList.setHasFixedSize(false);
        _binding.includes.newsDetailList.setLayoutManager(new LinearLayoutManager(this));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new NewsDetailRecyclerAdapter(this);
        _binding.includes.newsDetailList.setAdapter(_recyclerViewAdapter);

        // Put the first item in the recycler view.
        _recyclerViewAdapter.merge(newsItem, true);

        // Set the swipe refresh action.
        _binding.includes.swipeRefreshLayout.setOnRefreshListener(() -> {
            _eventBus.post(new HideResponseLayout());
            mergeQueuedItemsIntoList();
            Disposable disposable =
                    _shareItemService.fetchItemHeadersForParentAsync(
                            NewsItemDetailActivity.class,
                            _prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentPassword(),
                            newsItem.getRemoteId());
            _compositeDisposable.add(disposable);
        });

        // Go get the children.
        _binding.includes.swipeRefreshLayout.setRefreshing(true);
        mergeQueuedItemsIntoList();
        Disposable disposable =
                _shareItemService.fetchItemHeadersForParentAsync(NewsItemDetailActivity.class,
                        _prefsUtil.getCurrentUserAccountId(),
                        _prefsUtil.getCurrentPassword(),
                        newsItem.getRemoteId());
        _compositeDisposable.add(disposable);
    }

    private void mergeQueuedItemsIntoList() {

        RegularNewsListItem newsItem = (RegularNewsListItem) getIntent().getSerializableExtra(EXTRA_NEWS_ITEM);

        // First remove all local items from the list.
        _recyclerViewAdapter.removeLocalItems();

        // Fetch the queued items from the data store.
        Result<Share> shareItems =
                _shareDAO.list(_prefsUtil.getCurrentUserAccountId(), newsItem.getRemoteId());

        // Add the queued items to the adapter.
        List<AbstractNewsListItem> newsItems = new ArrayList<>();
        for (Share shareItem : shareItems) {

            switch (shareItem.getType()) {
                case ResponseV1:
                    ResponseNewsListItem newsListItem = new ResponseNewsListItem();
                    newsListItem.setLocalId(shareItem.getId());
                    newsListItem.setRemoteId(null);
                    newsListItem.setOwnerGuid(shareItem.getUserAccount().getGuid());
                    newsListItem.setParentItemRemoteId(shareItem.getParentItemRemoteId());
                    newsListItem.setItemTimestamp(shareItem.getTimestampGmt());
                    switch (shareItem.getState()) {
                        case Queued:
                            newsListItem.setState(NewsListItemState.Queued);
                            break;
                        case Error:
                            newsListItem.setState(NewsListItemState.SubmissionError);
                            break;
                        case Submitting:
                            newsListItem.setState(NewsListItemState.Submitting);
                            break;
                    }
                    newsListItem.setMessage(shareItem.getMessage());
                    newsItems.add(newsListItem);
                    break;
            }
        }
        _recyclerViewAdapter.merge(newsItems);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register to listen for events.
        if (!_eventBus.isRegistered(this)) {
            _eventBus.register(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _compositeDisposable.dispose();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_news_item_detail, menu);

        boolean skipTutorial = getResources().getBoolean(R.bool.skip_showcase_tutorial);
        if (!skipTutorial) {
            new Handler().post(() -> {
                ShowcaseConfig config = new ShowcaseConfig();
                config.setRenderOverNavigationBar(true);
                config.setDelay(100);
                MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID);
                sequence.setConfig(config);
                sequence.addSequenceItem(_binding.showcaseViewAnchor,
                        "This screen shows the shared item along with any responses that have been posted.\n\n" +
                                "If there is an image, you may tap on it to see the full-resolution picture.\n\n" +
                                "You may delete the item if it is yours, add the author as a friend, ignore the item, or post a reply.\n\n" +
                                "You may also reply to any responses by tapping on the response, or long-press on the response to get more options.", "GOT IT");
                sequence.start();
            });
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());
        RegularNewsListItem newsItem =
                (RegularNewsListItem) getIntent().getSerializableExtra(EXTRA_NEWS_ITEM);

        boolean newsItemOwnedByMe = newsItem.getOwnerGuid().compareTo(userAccount.getGuid()) == 0;

        // The delete item is enabled if the item is owned by me and it isn't already deleted.
        MenuItem delete = menu.findItem(R.id.action_delete);
        delete.setEnabled(newsItemOwnedByMe &&
                (newsItem.getState() == NewsListItemState.SubmissionError ||
                        newsItem.getState() == NewsListItemState.Decrypted));

        // The add friend is enabled if the item is owned by not me and not someone in my
        // friends list.
        MenuItem addFriend = menu.findItem(R.id.action_add_friend);
        boolean addFriendEnabled = false;
        if (newsItem.getOwnerGuid().compareTo(userAccount.getGuid()) != 0) {
            Friend friend = _friendDAO.find(_prefsUtil.getCurrentUserAccountId(),
                    newsItem.getOwnerGuid());
            if (friend == null) {
                addFriendEnabled = true;
            }
        }
        addFriend.setEnabled(addFriendEnabled);

        // The ignore item is enabled if the item is not owned by me.
        MenuItem ignore = menu.findItem(R.id.action_ignore);
        ignore.setEnabled(!newsItemOwnedByMe);

        // There are three submenu items under ignore -- ignore author, ignore item, and blacklist
        // author. Set them up. The ignore item is enabled if the item is not owned by me.
        MenuItem ignoreAuthor = ignore.getSubMenu().findItem(R.id.action_ignore_author);
        MenuItem ignoreItem = ignore.getSubMenu().findItem(R.id.action_ignore_item);
        MenuItem blacklistAuthor = ignore.getSubMenu().findItem(R.id.action_blacklist_author);
        final boolean isAuthorIgnored = _ignoreService.isIgnored(newsItem.getOwnerGuid());
        final boolean isItemIgnored = _ignoreService.isIgnored(newsItem.getRemoteId());
        final boolean isAuthorBlacklisted = newsItem.getState() == NewsListItemState.Blacklisted;

        // Set the text -- will be "ignore" or "unignore"
        if (isAuthorIgnored) {
            ignoreAuthor.setTitle(getString(R.string.app_text_unignore_author));
        } else {
            ignoreAuthor.setTitle(getString(R.string.app_text_ignore_author));
        }

        if (isItemIgnored) {
            ignoreItem.setTitle(getString(R.string.app_text_unignore_item));
        } else {
            ignoreItem.setTitle(getString(R.string.app_text_ignore_item));
        }

        blacklistAuthor.setEnabled(!isAuthorBlacklisted);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed()");
        if (_binding.includes.responseLayout.getVisibility() == View.VISIBLE) {
            _eventBus.post(new HideResponseLayout());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        RegularNewsListItem newsItem =
                (RegularNewsListItem) getIntent().getSerializableExtra(EXTRA_NEWS_ITEM);

        if (id == android.R.id.home) {

            _binding.includes.responseLayout.setVisibility(View.GONE);
            onBackPressed();
            return true;

        } else if (id == R.id.action_reply) {

            _eventBus.post(new ShowResponseLayout(0));
            return true;

        } else if (id == R.id.action_ignore_author) {

            final boolean isAuthorIgnored = _ignoreService.isIgnored(newsItem.getOwnerGuid());
            if (isAuthorIgnored) {
                _ignoreService.unignore(newsItem.getOwnerGuid());
            } else {
                _ignoreService.ignore(newsItem.getOwnerGuid());
            }
            _eventBus.post(new IgnoresChanged());
            return true;

        } else if (id == R.id.action_ignore_item) {

            final boolean isItemIgnored = _ignoreService.isIgnored(newsItem.getRemoteId());
            if (isItemIgnored) {
                _ignoreService.unignore(newsItem.getRemoteId());
            } else {
                _ignoreService.ignore(newsItem.getRemoteId());
            }
            _eventBus.post(new IgnoresChanged());
            return true;

        } else if (id == R.id.action_blacklist_author) {

            _confirmationDialogUtil.showConfirmationDialog(getSupportFragmentManager(),
                    R.string.dialog_confirm_title_blacklist_author,
                    R.string.dialog_confirm_text_blacklist_author,
                    "blacklistAuthorConfirmation",
                    () -> {
                        _userAccountService.blacklistAuthorAsync(_prefsUtil.getCurrentUserAccountId(),
                                _prefsUtil.getCurrentPassword(),
                                newsItem.getOwnerGuid());
                    });
            return true;

        } else if (id == R.id.action_add_friend) {

            // Open a dialog to allow the user to enter the name of the friend.
            AddFriendDialog dialog = AddFriendDialog.newInstance(
                    _prefsUtil.getCurrentUserAccountId(),
                    newsItem.getOwnerGuid());
            dialog.show(NewsItemDetailActivity.this.getSupportFragmentManager(), "addFriendDialog");
            return true;

        } else if (id == R.id.action_delete) {

            _confirmationDialogUtil.showConfirmationDialog(getSupportFragmentManager(),
                    R.string.dialog_confirm_title_delete_item,
                    R.string.dialog_confirm_text_delete_item,
                    "deleteItemConfirmation",
                    () -> _shareItemService.deleteItemAsync(newsItem.getRemoteId(),
                            _prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentPassword()));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ShareService.REQUEST_EDIT_RESPONSE_ITEM &&
                (resultCode == ShareItemActivity.RESULT_DELETED ||
                        resultCode == ShareItemActivity.RESULT_SUBMITTED)) {
            mergeQueuedItemsIntoList();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void submitResponse(View view) {

        RegularNewsListItem newsItem = (RegularNewsListItem) getIntent().getSerializableExtra(EXTRA_NEWS_ITEM);
        Long parentItemRemoteId;
        if (_selectedItemRemoteId != null) {
            parentItemRemoteId = _selectedItemRemoteId;
        } else {
            parentItemRemoteId = newsItem.getRemoteId();
        }

        // Queue the item and refresh the recycler view.
        long itemId = _shareItemService.queueItem(_prefsUtil.getCurrentUserAccountId(),
                parentItemRemoteId,
                newsItem.getRemoteId(),
                _binding.includes.responseText.getText().toString());
        _recyclerViewAdapter.notifyDataSetChanged();

        // Hide the keyboard.
        KeyboardUtil.hideSoftKeyboard(_binding.includes.responseText);
        _binding.includes.responseLayout.setVisibility(View.GONE);
        _binding.includes.responseText.clearFocus();

        // Let the user know the item has been queued and submit the item.
        Toast.makeText(this, R.string.share_item_toast_response_queued, Toast.LENGTH_LONG).show();
        Disposable disposable =
                _shareItemService.submitItemAsync(NewsItemDetailActivity.class,
                        itemId,
                        _prefsUtil.getCurrentPassword());
        _compositeDisposable.add(disposable);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SubmitItemStarted event) {
        _recyclerViewAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SubmitItemComplete event) {

        if (event.getEventTarget() != NewsItemDetailActivity.class) {
            return;
        }

        // Mostly interested in showing a toast indicating success or failure.
        if (event.getStatus() == AsyncResult.Success) {
            Toast.makeText(this,
                    R.string.item_detail_toast_response_submission_success,
                    Toast.LENGTH_LONG).show();

            // Go fetch only the newly submitted response so we can add it to the list.
            Disposable disposable =
                    _shareItemService.fetchItemHeaderAsync(NewsItemDetailActivity.class,
                            _prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentPassword(),
                            event.getItemRemoteId());
            _compositeDisposable.add(disposable);

        } else {

            // Show a toast.
            Toast.makeText(this,
                    R.string.item_detail_toast_response_submission_failed,
                    Toast.LENGTH_LONG).show();

        }

        mergeQueuedItemsIntoList();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ShowResponseLayout event) {
        _selectedItemRemoteId = event.getItemRemoteId();
        _binding.includes.responseLayout.setVisibility(View.VISIBLE);
        _binding.includes.responseText.setText(null);
        KeyboardUtil.showSoftKeyboard(_binding.includes.responseText);

        new Handler().postDelayed(() -> _binding.includes.newsDetailList.scrollToPosition(event.getItemPosition()), 150);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(HideResponseLayout event) {
        KeyboardUtil.hideSoftKeyboard(_binding.includes.responseText);
        _binding.includes.responseLayout.setVisibility(View.GONE);
        _binding.includes.responseText.clearFocus();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ScrollToResponse event) {
        _binding.includes.newsDetailList.scrollToPosition(event.getPosition());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FetchItemHeadersComplete event) {

        if (event.getEventTarget() != NewsItemDetailActivity.class) {
            return;
        }

        if (_binding.includes.swipeRefreshLayout.isRefreshing()) {
            _binding.includes.swipeRefreshLayout.setRefreshing(false);
        }

        if (event.getStatus() == AsyncResult.Success) {

            // Add the headers to the list. They'll get decrypted by and by.

            // Turn the share item into response items (as far as is possible without decryption)
            // and put them in the list.
            List<AbstractNewsListItem> newsItems = new ArrayList<>();
            for (ShareItem shareItem : event.getShareItems()) {
                ResponseNewsListItem responseItem = new ResponseNewsListItem();

                if (shareItem.isBlacklisted()) {
                    responseItem.setState(NewsListItemState.Blacklisted);
                } else if (shareItem.isMarkedAsDeleted()) {
                    responseItem.setState(NewsListItemState.Deleted);
                } else {
                    responseItem.setState(NewsListItemState.Decrypting);
                }
                responseItem.setRemoteId(shareItem.getRemoteId());
                responseItem.setItemTimestamp(shareItem.getItemTimestamp());
                responseItem.setOwnerGuid(shareItem.getOwnerGuid());
                responseItem.setParentItemRemoteId(shareItem.getParentItemRemoteId());
                newsItems.add(responseItem);
            }
            _recyclerViewAdapter.merge(newsItems);

            // Issue calls to decrypt the share items, but only for non-deleted, non-blacklisted
            // items.
            for (ShareItem shareItem : event.getShareItems()) {
                if (!shareItem.isMarkedAsDeleted() && !shareItem.isBlacklisted()) {
                    Disposable disposable =
                            _shareItemService.decryptShareItemHeaderAsync(NewsItemDetailActivity.class,
                                    _prefsUtil.getCurrentUserAccountId(),
                                    shareItem);
                    _compositeDisposable.add(disposable);
                }
            }

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            boolean forceSignOut = false;
            switch (event.getStatus()) {
                case IncorrectPassword:
                case Unauthorized:
                    errorMessageId = R.string.app_snack_error_unauthorized;
                    forceSignOut = true;
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
            }

            // Give the user a snack. Yum.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(NewsItemDetailActivity.this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            if (forceSignOut) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        _prefsUtil.clearCredentials();
                        Intent intent = new Intent(NewsItemDetailActivity.this, SelectUserAccountActivity.class);
                        startActivity(intent);
                        NewsItemDetailActivity.this.finish();
                    }
                });
            }
            snackbar.show();

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewsItemDecryptComplete event) {

        if (event.getEventTarget() != NewsItemDetailActivity.class) {
            return;
        }

        if (event.getStatus() == AsyncResult.Success) {
            // Merge the item into the recycler view.
            _recyclerViewAdapter.merge(event.getNewsItem(), true);
        } else {
            // Set the item state based on the decryption/deserialization result.
            switch (event.getStatus()) {
                case DecryptionFailed:
                    _recyclerViewAdapter.updateState(event.getNewsItemRemoteId(), NewsListItemState.DecryptionFailed);
                    break;
                case ItemDeserializationFailed:
                    _recyclerViewAdapter.updateState(event.getNewsItemRemoteId(), NewsListItemState.DeserializationFailed);
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DeleteItemComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            RegularNewsListItem rootItem = (RegularNewsListItem) getIntent().getSerializableExtra(EXTRA_NEWS_ITEM);
            if (rootItem.getRemoteId().compareTo(event.getItemRemoteId()) == 0) {

                rootItem.setMessage(null);
                rootItem.setThumbnailData(null);
                rootItem.setTitle(null);
                rootItem.setUrl(null);
                rootItem.setState(NewsListItemState.Deleted);
                _recyclerViewAdapter.merge(rootItem, true);
                invalidateOptionsMenu();

            } else {

                Disposable disposable =
                        _shareItemService.fetchItemHeaderAsync(NewsItemDetailActivity.class,
                                _prefsUtil.getCurrentUserAccountId(),
                                _prefsUtil.getCurrentPassword(),
                                event.getItemRemoteId());
                _compositeDisposable.add(disposable);

            }

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            boolean forceSignOut = false;
            switch (event.getStatus()) {
                case IncorrectPassword:
                case Unauthorized:
                    errorMessageId = R.string.app_snack_error_unauthorized;
                    forceSignOut = true;
                    break;
                case NotFound:
                    errorMessageId = R.string.fragment_news_snack_error_delete_item_not_found;
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
            }

            // Give the user a snack.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout, errorMessageId, snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(NewsItemDetailActivity.this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            if (forceSignOut) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        _prefsUtil.clearCredentials();
                        Intent intent = new Intent(NewsItemDetailActivity.this, SelectUserAccountActivity.class);
                        startActivity(intent);
                        NewsItemDetailActivity.this.finish();
                    }
                });
            }
            snackbar.show();

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AddFriendComplete event) {
        if (event.getStatus() == AsyncResult.Success) {
            invalidateOptionsMenu();
            _recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(IgnoresChanged event) {
        invalidateOptionsMenu();
        _recyclerViewAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BlacklistAuthorComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            RegularNewsListItem rootItem = (RegularNewsListItem) getIntent().getSerializableExtra(EXTRA_NEWS_ITEM);
            if (rootItem.getOwnerGuid().compareTo(event.getGuid()) == 0) {
                rootItem.setMessage(null);
                rootItem.setThumbnailData(null);
                rootItem.setTitle(null);
                rootItem.setUrl(null);
                rootItem.setState(NewsListItemState.Blacklisted);
                invalidateOptionsMenu();
            }

            _binding.includes.swipeRefreshLayout.setRefreshing(true);
            _eventBus.post(new HideResponseLayout());
            _recyclerViewAdapter.clearModel();
            _recyclerViewAdapter.merge(rootItem, true);
            mergeQueuedItemsIntoList();
            Disposable disposable =
                    _shareItemService.fetchItemHeadersForParentAsync(
                            NewsItemDetailActivity.class,
                            _prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentPassword(),
                            rootItem.getRemoteId());
            _compositeDisposable.add(disposable);

        } else {

            // Map the status to an error message.
            @StringRes
            int errorMessageId = 0;
            Integer helpResId = null;
            int snackbarLength = Snackbar.LENGTH_LONG;
            boolean forceSignOut = false;
            switch (event.getStatus()) {
                case IncorrectPassword:
                case Unauthorized:
                    errorMessageId = R.string.app_snack_error_unauthorized;
                    forceSignOut = true;
                    break;
                case NotFound:
                    errorMessageId = R.string.fragment_news_snack_error_delete_item_not_found;
                    break;
                case TooManyRequests:
                    errorMessageId = R.string.app_snack_error_too_many_requests;
                    helpResId = R.raw.help_error_too_many_requests;
                    break;
                case OtherHttpError:
                    errorMessageId = R.string.app_snack_error_other_http;
                    break;
                case ServerUnavailable:
                    errorMessageId = R.string.app_snack_server_unavailable;
                    helpResId = R.raw.help_error_server_unavailable;
                    break;
                case RetrofitIOException:
                    errorMessageId = R.string.app_snack_error_retrofit_io;
                    helpResId = R.raw.help_error_retrofit_io;
                    break;
            }

            // Give the user a snack.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout, errorMessageId, snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(NewsItemDetailActivity.this, HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            if (forceSignOut) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        _prefsUtil.clearCredentials();
                        Intent intent = new Intent(NewsItemDetailActivity.this, SelectUserAccountActivity.class);
                        startActivity(intent);
                        NewsItemDetailActivity.this.finish();
                    }
                });
            }
            snackbar.show();
        }
    }
}
