package com.aptasystems.kakapo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.aptasystems.kakapo.HelpActivity;
import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.MainActivity;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.SelectUserAccountActivity;
import com.aptasystems.kakapo.ShareItemActivity;
import com.aptasystems.kakapo.adapter.NewsRecyclerAdapter;
import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.adapter.model.NewsListItemState;
import com.aptasystems.kakapo.adapter.model.RegularNewsListItem;
import com.aptasystems.kakapo.databinding.FragmentNewsBinding;
import com.aptasystems.kakapo.dialog.AddFriendDialog;
import com.aptasystems.kakapo.entities.CachedRegularItem;
import com.aptasystems.kakapo.entities.Share;
import com.aptasystems.kakapo.entities.ShareType;
import com.aptasystems.kakapo.event.AddFriendComplete;
import com.aptasystems.kakapo.event.AddFriendRequested;
import com.aptasystems.kakapo.event.BlacklistAuthorComplete;
import com.aptasystems.kakapo.event.DeleteItemComplete;
import com.aptasystems.kakapo.event.FetchItemHeadersComplete;
import com.aptasystems.kakapo.event.FetchItemHeadersRequested;
import com.aptasystems.kakapo.event.FriendColourChanged;
import com.aptasystems.kakapo.event.FriendDeleted;
import com.aptasystems.kakapo.event.FriendRenamed;
import com.aptasystems.kakapo.event.IgnoresChanged;
import com.aptasystems.kakapo.event.NewsFilterApplied;
import com.aptasystems.kakapo.event.NewsItemDecryptComplete;
import com.aptasystems.kakapo.event.QueuedItemDeleted;
import com.aptasystems.kakapo.event.ShareItemQueued;
import com.aptasystems.kakapo.event.SubmitItemComplete;
import com.aptasystems.kakapo.event.SubmitItemStarted;
import com.aptasystems.kakapo.event.UserAccountColourChanged;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.util.FilterType;
import com.aptasystems.kakapo.viewmodel.NewsFragmentModel;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.requery.query.Result;
import kakapo.api.model.ShareItem;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class NewsFragment extends BaseFragment {

    private static final String TAG = NewsFragment.class.getSimpleName();
    private static final String SHOWCASE_ID = NewsFragment.class.getSimpleName();

    public static NewsFragment newInstance() {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private NewsRecyclerAdapter _recyclerViewAdapter;
    private CompositeDisposable _compositeDisposable = new CompositeDisposable();
    private FragmentNewsBinding _binding;

    public NewsFragment() {
        // Required no argument public constructor.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ((KakapoApplication) getActivity().getApplication()).getKakapoComponent().inject(this);

        // Register to listen for events.
        if (!_eventBus.isRegistered(this)) {
            _eventBus.register(this);
        }
    }

    @Override
    public void onStart() {

        final NewsFragmentModel viewModel = new ViewModelProvider(this)
                .get(NewsFragmentModel.class);

        // Observe changes in the quota and update the UI.
        viewModel.getRemainingItemCountLiveData().observe(this, remainingItemCount -> {
            if (remainingItemCount != null) {
                _recyclerViewAdapter.updateRemainingItemCount(remainingItemCount);
            }
        });

        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        _binding = FragmentNewsBinding.inflate(inflater, container, false);

        // If we don't have authentication info, just stop. The main activity will redirect us
        // to the sign in activity.
        if (_prefsUtil.getCurrentUserAccountId() == null &&
                _prefsUtil.getCurrentHashedPassword() == null) {
            return _binding.getRoot();
        }

        // On click listeners.
        _binding.addFloatingButton.setOnClickListener(this::addShareItem);

        // Restore state if available.
        List<AbstractNewsListItem> cachedNewsItems = new ArrayList<>();
        if (savedInstanceState != null) {
            Result<CachedRegularItem> cachedItems = _entityStore.select(CachedRegularItem.class)
                    .orderBy(CachedRegularItem.REMOTE_ID.asc())
                    .get();
            for (CachedRegularItem cachedItem : cachedItems) {
                RegularNewsListItem newsListItem = new RegularNewsListItem();
                newsListItem.setRemoteId(cachedItem.getRemoteId());
                newsListItem.setOwnerGuid(cachedItem.getOwnerGuid());
                newsListItem.setItemTimestamp(cachedItem.getItemTimestamp());
                newsListItem.setState(cachedItem.getState());
                newsListItem.setThumbnailData(cachedItem.getThumbnailData());
                newsListItem.setTitle(cachedItem.getTitle());
                newsListItem.setUrl(cachedItem.getUrl());
                newsListItem.setMessage(cachedItem.getMessage());
                newsListItem.setChildCount(cachedItem.getChildCount());
                cachedNewsItems.add(newsListItem);
            }
        }

        // Set up the recycler view.
        _binding.newsList.setHasFixedSize(false);
        _binding.newsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new NewsRecyclerAdapter(getActivity());
        _binding.newsList.setAdapter(_recyclerViewAdapter);

        _recyclerViewAdapter.merge(cachedNewsItems);
        _recyclerViewAdapter.filter(true);

        // Set the swipe refresh action to refetch the news items.
        _binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            _recyclerViewAdapter.truncateModel();
            mergeQueuedItemsIntoList();
            _shareItemService.fetchItemHeadersAsync(NewsFragment.class,
                    _prefsUtil.getCurrentUserAccountId(),
                    _prefsUtil.getCurrentHashedPassword(),
                    Long.MAX_VALUE);
        });

        // Go fetch some items if we need to.
        if (cachedNewsItems.isEmpty()) {
            _binding.swipeRefreshLayout.setRefreshing(true);
            _shareItemService.fetchItemHeadersAsync(NewsFragment.class,
                    _prefsUtil.getCurrentUserAccountId(),
                    _prefsUtil.getCurrentHashedPassword(),
                    Long.MAX_VALUE);
        }
        mergeQueuedItemsIntoList();

        return _binding.getRoot();
    }

    private void mergeQueuedItemsIntoList() {

        // First remove all local items from the list.
        _recyclerViewAdapter.removeLocalItems();

        // Fetch the queued items from the data store.
        Result<Share> shareItems = _entityStore.select(Share.class)
                .where(Share.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .orderBy(Share.TIMESTAMP_GMT.asc())
                .get();

        // Add the queued items to the adapter.
        for (Share shareItem : shareItems) {

            switch (shareItem.getType()) {
                case RegularV1:

                    RegularNewsListItem newsListItem = new RegularNewsListItem();
                    newsListItem.setLocalId(shareItem.getId());
                    newsListItem.setRemoteId(null);
                    newsListItem.setOwnerGuid(shareItem.getUserAccount().getGuid());
                    newsListItem.setParentItemRemoteId(null);
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
                    newsListItem.setTitle(shareItem.getTitle());
                    newsListItem.setUrl(shareItem.getUrl());
                    byte[] thumbnailData = new byte[0];
                    try {
                        thumbnailData = shareItem.extractThumbnailData(getContext());
                    } catch (IOException e) {
                        // Log and carry on.
                        Log.e(TAG, "SubmissionError extracting thumbnail data from queued item", e);
                    }
                    newsListItem.setThumbnailData(thumbnailData);
                    newsListItem.setMessage(shareItem.getMessage());
                    newsListItem.setChildCount(0);
                    _recyclerViewAdapter.merge(newsListItem);
                    break;
            }
        }

        _recyclerViewAdapter.filter(true);
        showHideNoItemsView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // First remove all the cached regular items from the datastore.
        _entityStore.delete(CachedRegularItem.class)
                .get()
                .value();

        // Then create cached regular items from the values in the recycler view.
        for (AbstractNewsListItem newsItem : _recyclerViewAdapter.getModel()) {
            RegularNewsListItem regularNewsListItem = (RegularNewsListItem) newsItem;
            if (newsItem != null && newsItem.getRemoteId() != null) {
                CachedRegularItem cachedItem = new CachedRegularItem();
                cachedItem.setRemoteId(newsItem.getRemoteId());
                cachedItem.setOwnerGuid(newsItem.getOwnerGuid());
                cachedItem.setItemTimestamp(newsItem.getItemTimestamp());
                cachedItem.setState(newsItem.getState());
                cachedItem.setThumbnailData(regularNewsListItem.getThumbnailData());
                cachedItem.setTitle(regularNewsListItem.getTitle());
                cachedItem.setUrl(regularNewsListItem.getUrl());
                cachedItem.setMessage(regularNewsListItem.getMessage());
                cachedItem.setChildCount(regularNewsListItem.getChildCount());
                _entityStore.insert(cachedItem);
            }
        }
    }

    @Override
    public void onDestroyView() {
        _binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }

        _compositeDisposable.dispose();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_news, menu);

        new Handler().post(() -> {

            ShowcaseConfig config = new ShowcaseConfig();
            config.setRenderOverNavigationBar(true);
            config.setDelay(100);
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), SHOWCASE_ID);
            sequence.setConfig(config);
            sequence.addSequenceItem(_binding.showcaseViewAnchor, "This is your news feed. It lists things that you have shared and that others have shared with you. To fetch an updated list of news, swipe down at the top of the list.", "GOT IT");
            sequence.addSequenceItem(_binding.addFloatingButton, "Use the add button to share something with your friends.", "GOT IT");

            sequence.start();
        });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        EnumSet<FilterType> filters = _prefsUtil.getFilterTypes();
        menu.findItem(R.id.action_filter_mine).setChecked(filters.contains(FilterType.Mine));
        menu.findItem(R.id.action_filter_friends).setChecked(filters.contains(FilterType.Friends));
        menu.findItem(R.id.action_filter_strangers).setChecked(filters.contains(FilterType.Strangers));
        menu.findItem(R.id.action_filter_deleted).setChecked(filters.contains(FilterType.Deleted));
        menu.findItem(R.id.action_filter_ignored).setChecked(filters.contains(FilterType.Ignored));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // If a filter option is selected, we set the filter preference, invalidate the options menu,
        // and perform a clear and refresh of the news items in the list.

        switch (item.getItemId()) {
            case R.id.action_filter_mine:
                toggleFilter(FilterType.Mine);
                return true;
            case R.id.action_filter_friends:
                toggleFilter(FilterType.Friends);
                return true;
            case R.id.action_filter_strangers:
                toggleFilter(FilterType.Strangers);
                return true;
            case R.id.action_filter_deleted:
                toggleFilter(FilterType.Deleted);
                return true;
            case R.id.action_filter_ignored:
                toggleFilter(FilterType.Ignored);
                return true;
        }
        return false;
    }

    private void toggleFilter(FilterType filterType) {
        boolean currentlyOn = _prefsUtil.getFilterTypes().contains(filterType);

        // Add or remove the filter in the prefs.
        if (currentlyOn) {
            _prefsUtil.removeFilter(filterType);
        } else {
            _prefsUtil.addFilter(filterType);
        }

        // Filter the recycler view.
        getActivity().invalidateOptionsMenu();
        _recyclerViewAdapter.filter(true);
        showHideNoItemsView();
    }

    private void showHideNoItemsView() {
        _binding.emptyListView.setVisibility(_recyclerViewAdapter.isListEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addShareItem(View view) {
        Intent intent = new Intent(getActivity(), ShareItemActivity.class);
        intent.putExtra(ShareItemActivity.EXTRA_KEY_ITEM_TYPE, ShareType.RegularV1);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UserAccountColourChanged event) {
        _recyclerViewAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendColourChanged event) {
        _recyclerViewAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendDeleted event) {
        _recyclerViewAdapter.filter(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AddFriendComplete event) {
        // Only care about success.
        if (event.getStatus() == AsyncResult.Success) {
            _recyclerViewAdapter.filter(true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SubmitItemStarted event) {
        mergeQueuedItemsIntoList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SubmitItemComplete event) {

        if (event.getEventTarget() != MainActivity.class) {
            return;
        }

        if (event.getStatus() == AsyncResult.Success) {
            // If successful, go fetch the new item.
            Disposable disposable =
                    _shareItemService.fetchItemHeaderAsync(NewsFragment.class,
                            _prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentHashedPassword(),
                            event.getItemRemoteId());
            _compositeDisposable.add(disposable);
        }

        mergeQueuedItemsIntoList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DeleteItemComplete event) {

        // If successful, go fetch the new (truncated) item.
        if (event.getStatus() == AsyncResult.Success) {
            Disposable disposable =
                    _shareItemService.fetchItemHeaderAsync(NewsFragment.class,
                            _prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentHashedPassword(),
                            event.getItemRemoteId());
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
            Snackbar snackbar = Snackbar.make(getView(), errorMessageId, snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(getActivity(), HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            if (forceSignOut) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        _prefsUtil.clearCredentials();
                        Intent intent = new Intent(getActivity(), SelectUserAccountActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
            }
            snackbar.show();

        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ShareItemQueued event) {
        mergeQueuedItemsIntoList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FetchItemHeadersComplete event) {

        if (event.getEventTarget() != NewsFragment.class) {
            return;
        }

        if (_binding.swipeRefreshLayout.isRefreshing()) {
            _binding.swipeRefreshLayout.setRefreshing(false);
        }

        // Add the headers to the list. They'll get decrypted by and by.

        if (event.getStatus() == AsyncResult.Success) {

            for (ShareItem shareItem : event.getShareItems()) {

                // Turn the share item into a news item (as far as is possible without decryption)
                // and put it in the list.
                RegularNewsListItem newsItem = new RegularNewsListItem();
                newsItem.setState(shareItem.isMarkedAsDeleted() ?
                        NewsListItemState.Deleted : NewsListItemState.Decrypting);
                newsItem.setRemoteId(shareItem.getRemoteId());
                newsItem.setItemTimestamp(shareItem.getItemTimestamp());
                newsItem.setOwnerGuid(shareItem.getOwnerGuid());
                newsItem.setParentItemRemoteId(shareItem.getParentItemRemoteId());
                newsItem.setChildCount(shareItem.getChildCount());
                _recyclerViewAdapter.merge(newsItem);

                if (!shareItem.isMarkedAsDeleted()) {
                    Disposable disposable =
                            _shareItemService.decryptShareItemHeaderAsync(NewsFragment.class,
                                    _prefsUtil.getCurrentUserAccountId(),
                                    _prefsUtil.getCurrentHashedPassword(),
                                    shareItem);
                    _compositeDisposable.add(disposable);
                }
            }

            // Run the filter.
            _recyclerViewAdapter.filter(true);

            // If no items made the filter and there are more to go get, go get some more.
            if (_recyclerViewAdapter.isListEmpty() && event.getRemainingItemCount() > 0) {
                long lowItemRid = Long.MAX_VALUE;
                for (AbstractNewsListItem entity : _recyclerViewAdapter.getModel()) {
                    if (entity.getRemoteId() != null) {
                        lowItemRid = Math.min(lowItemRid, entity.getRemoteId());
                    }
                }
                _eventBus.post(new FetchItemHeadersRequested(lowItemRid));
            }

            if (event.getRemainingItemCount() != null && event.getRemainingItemCount() > -1L) {
                final NewsFragmentModel viewModel = new ViewModelProvider(this)
                        .get(NewsFragmentModel.class);
                viewModel.getRemainingItemCountLiveData().setValue(event.getRemainingItemCount());
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

            // Give the user a snack.
            Snackbar snackbar = Snackbar.make(_binding.coordinatorLayout,
                    errorMessageId,
                    snackbarLength);
            if (helpResId != null) {
                final int finalHelpResId = helpResId;
                snackbar.setAction(R.string.app_action_more_info, v -> {
                    Intent intent = new Intent(getActivity(), HelpActivity.class);
                    intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, finalHelpResId);
                    startActivity(intent);
                });
            }
            if (forceSignOut) {
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        _prefsUtil.clearCredentials();
                        Intent intent = new Intent(getActivity(), SelectUserAccountActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
            }
            snackbar.show();
        }

        showHideNoItemsView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewsItemDecryptComplete event) {

        if (event.getEventTarget() != NewsFragment.class) {
            return;
        }

        if (event.getStatus() == AsyncResult.Success) {

            // Merge the item into the recycler view.
            _recyclerViewAdapter.merge(event.getNewsItem());
            _recyclerViewAdapter.filter(true);
            showHideNoItemsView();

        } else {

            // Set the item state based on the decryption/deserialization result.
            switch (event.getStatus()) {
                case DecryptionFailed:
                    _recyclerViewAdapter.updateState(event.getNewsItemRemoteId(),
                            NewsListItemState.DecryptionFailed);
                    _recyclerViewAdapter.filter(true);
                    break;
                case ItemDeserializationFailed:
                    _recyclerViewAdapter.updateState(event.getNewsItemRemoteId(),
                            NewsListItemState.DeserializationFailed);
                    _recyclerViewAdapter.filter(true);
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(QueuedItemDeleted event) {
        mergeQueuedItemsIntoList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FetchItemHeadersRequested event) {
        Disposable disposable =
                _shareItemService.fetchItemHeadersAsync(NewsFragment.class,
                        _prefsUtil.getCurrentUserAccountId(),
                        _prefsUtil.getCurrentHashedPassword(),
                        event.getLowItemRemoteId());
        _compositeDisposable.add(disposable);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AddFriendRequested event) {
        // Open a dialog to allow the user to enter the name of the friend.
        AddFriendDialog dialog = AddFriendDialog.newInstance(
                _prefsUtil.getCurrentUserAccountId(),
                event.getFriendGuid());
        dialog.show(getActivity().getSupportFragmentManager(), "addFriendDialog");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(IgnoresChanged event) {
        _recyclerViewAdapter.filter(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FriendRenamed event) {
        _recyclerViewAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewsFilterApplied event) {
        showHideNoItemsView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BlacklistAuthorComplete event) {
        _binding.swipeRefreshLayout.setRefreshing(true);
        _recyclerViewAdapter.truncateModel();
        mergeQueuedItemsIntoList();
        _shareItemService.fetchItemHeadersAsync(NewsFragment.class,
                _prefsUtil.getCurrentUserAccountId(),
                _prefsUtil.getCurrentHashedPassword(),
                Long.MAX_VALUE);
    }
}
