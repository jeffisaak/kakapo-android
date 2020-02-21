package com.aptasystems.kakapo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.adapter.GroupRecyclerAdapter;
import com.aptasystems.kakapo.dialog.AddGroupDialog;
import com.aptasystems.kakapo.event.GroupAdded;
import com.aptasystems.kakapo.event.GroupsListModelChanged;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class GroupListFragment extends BaseFragment {

    private static final String TAG = GroupListFragment.class.getSimpleName();
    private static final String SHOWCASE_ID = GroupListFragment.class.getSimpleName();

    public static GroupListFragment newInstance() {
        GroupListFragment fragment = new GroupListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.showcase_view_anchor)
    FrameLayout _showcaseViewAnchor;

    @BindView(R.id.recycler_view_friend_list)
    RecyclerView _recyclerView;

    @BindView(R.id.floating_button_add_group)
    FloatingActionButton _addFloatingActionButton;

    @BindView(R.id.fragment_group_list_text_view_no_items)
    TextView _noItemsView;

    private GroupRecyclerAdapter _recyclerViewAdapter;

    public GroupListFragment() {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_group_list, container, false);

        // If we don't have authentication info, just stop. The main activity will redirect us
        // to the sign in activity.
        if (_prefsUtil.getCurrentUserAccountId() == null && _prefsUtil.getCurrentHashedPassword() == null) {
            return result;
        }

        // Bind.
        _unbinder = ButterKnife.bind(this, result);

        // Set up the recycler view.
        _recyclerView.setHasFixedSize(true);
        _recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new GroupRecyclerAdapter(getActivity());
        _recyclerView.setAdapter(_recyclerViewAdapter);
        _recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));

        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (_unbinder != null) {
            _unbinder.unbind();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop listening for events.
        if (_eventBus.isRegistered(this)) {
            _eventBus.unregister(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the list.
        _recyclerViewAdapter.refresh();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        new Handler().post(() -> {
            ShowcaseConfig config = new ShowcaseConfig();
            config.setRenderOverNavigationBar(true);
            config.setDelay(100);
            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), SHOWCASE_ID);
            sequence.setConfig(config);
            sequence.addSequenceItem(_showcaseViewAnchor,
                    "This is your groups list. You can create and delete groups, and add and remove friends from groups.", "GOT IT");
            sequence.addSequenceItem(_addFloatingActionButton,
                    "Use the add button to add a group.",
                    "GOT IT");
            sequence.start();
        });
    }

    @OnClick(R.id.floating_button_add_group)
    public void addGroup(View view) {
        AddGroupDialog dialog = AddGroupDialog.newInstance(_prefsUtil.getCurrentUserAccountId());
        dialog.show(getActivity().getSupportFragmentManager(), "addGroupDialog");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(GroupsListModelChanged event) {
        _recyclerView.setVisibility(event.getNewItemCount() == 0 ? View.GONE : View.VISIBLE);
        _noItemsView.setVisibility(event.getNewItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final GroupAdded event) {
        _recyclerViewAdapter.refresh();
    }

}