package com.aptasystems.kakapo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.adapter.GroupRecyclerAdapter;
import com.aptasystems.kakapo.databinding.FragmentGroupListBinding;
import com.aptasystems.kakapo.dialog.AddGroupDialog;
import com.aptasystems.kakapo.event.GroupAdded;
import com.aptasystems.kakapo.event.GroupsListModelChanged;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
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

    private GroupRecyclerAdapter _recyclerViewAdapter;
    private FragmentGroupListBinding _binding;

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
        _binding = FragmentGroupListBinding.inflate(inflater, container, false);

        // If we don't have authentication info, just stop. The main activity will redirect us
        // to the sign in activity.
        if (_prefsUtil.getCurrentUserAccountId() == null && _prefsUtil.getCurrentHashedPassword() == null) {
            return _binding.getRoot();
        }

        // On click listeners.
        _binding.floatingButtonAddGroup.setOnClickListener(this::addGroup);

        // Set up the recycler view.
        _binding.recyclerViewFriendList.setHasFixedSize(true);
        _binding.recyclerViewFriendList.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the recycler view adapter.
        _recyclerViewAdapter = new GroupRecyclerAdapter(getActivity());
        _binding.recyclerViewFriendList.setAdapter(_recyclerViewAdapter);
        _binding.recyclerViewFriendList.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));

        return _binding.getRoot();
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
            sequence.addSequenceItem(_binding.showcaseViewAnchor,
                    "This is your groups list. You can create and delete groups, and add and remove friends from groups.", "GOT IT");
            sequence.addSequenceItem(_binding.floatingButtonAddGroup,
                    "Use the add button to add a group.",
                    "GOT IT");
            sequence.start();
        });
    }

    public void addGroup(View view) {
        AddGroupDialog dialog = AddGroupDialog.newInstance(_prefsUtil.getCurrentUserAccountId());
        dialog.show(getActivity().getSupportFragmentManager(), "addGroupDialog");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(GroupsListModelChanged event) {
        _binding.recyclerViewFriendList.setVisibility(event.getNewItemCount() == 0 ? View.GONE : View.VISIBLE);
        _binding.fragmentGroupListTextViewNoItems.setVisibility(event.getNewItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final GroupAdded event) {
        _recyclerViewAdapter.refresh();
    }

}