package com.aptasystems.kakapo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.aptasystems.kakapo.databinding.ActivityShareItemBinding;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.entities.GroupMember;
import com.aptasystems.kakapo.entities.ShareType;
import com.aptasystems.kakapo.event.QueuedItemDeleted;
import com.aptasystems.kakapo.service.AttachmentHandlingService;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.view.ShareTarget;
import com.google.android.material.snackbar.Snackbar;
import com.tokenautocomplete.FilteredArrayAdapter;

import org.greenrobot.eventbus.EventBus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import io.requery.Persistable;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import kakapo.util.StringUtil;

public class ShareItemActivity extends AppCompatActivity {

    private static final String TAG = ShareItemActivity.class.getSimpleName();

    private static final int REQUEST_SELECT_ATTACHMENT = 100;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 101;

    public static final int RESULT_DELETED = RESULT_FIRST_USER;
    public static final int RESULT_SUBMITTED = RESULT_FIRST_USER + 1;

    private static final String STATE_KEY_SELECTED_ATTACHMENT_URI = "selectedAttachmentUri";
    private static final String STATE_KEY_SELECTED_MIME_TYPE = "mimeType";

    public static final String EXTRA_KEY_ITEM_TYPE = "itemType";
    public static final String EXTRA_KEY_ITEM_ID = "itemId";
    public static final String EXTRA_KEY_RECIPIENTS = "recipients";
    public static final String EXTRA_KEY_TITLE = "title";
    public static final String EXTRA_KEY_URL = "url";
    public static final String EXTRA_KEY_MESSAGE = "message";
    public static final String EXTRA_KEY_RESPONSE = "response";
    public static final String EXTRA_KEY_ATTACHMENT_URI = "attachmentUri";
    public static final String EXTRA_KEY_MIME_TYPE = "mimeType";
    public static final String EXTRA_KEY_ERROR_MESSAGE = "errorMessage";
    public static final String EXTRA_KEY_PARENT_ITEM_REMOTE_ID = "parentItemRemoteId";
    public static final String EXTRA_KEY_ROOT_ITEM_REMOTE_ID = "rootItemRemoteId";

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    EntityDataStore<Persistable> _entityStore;

    @Inject
    ShareService _shareItemService;

    @Inject
    AttachmentHandlingService _attachmentHandlingService;

    @Inject
    EventBus _eventBus;

    private Uri _selectedAttachmentUri;
    private String _mimeType;
    private ActivityShareItemBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        _binding = ActivityShareItemBinding.inflate(getLayoutInflater());
        setContentView(_binding.getRoot());
        setSupportActionBar(_binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Restore the selected image URI from state if available.
        if (savedInstanceState != null &&
                savedInstanceState.containsKey(STATE_KEY_SELECTED_ATTACHMENT_URI)) {
            _selectedAttachmentUri =
                    savedInstanceState.getParcelable(STATE_KEY_SELECTED_ATTACHMENT_URI);
            _mimeType = savedInstanceState.getString(STATE_KEY_SELECTED_MIME_TYPE);
        }

        if (getIntent().hasExtra(EXTRA_KEY_ITEM_TYPE)) {
            populateFieldsFromExtras();
        } else {
            // This is a regular share item, so set up the completion view where the user
            // selects friends and groups to share with and make the appropriate layout visible.

            _binding.includes.includeRegularItem.regularShareItemScrollView.setVisibility(View.VISIBLE);
            _binding.includes.includeResponseItem.responseShareItemScrollView.setVisibility(View.GONE);
            setupCompletionView();

        }
    }

    private void populateFieldsFromExtras() {
        ShareType itemType = (ShareType) getIntent().getSerializableExtra(EXTRA_KEY_ITEM_TYPE);

        // Populate and show the error message text view if appropriate.
        if (getIntent().hasExtra(EXTRA_KEY_ERROR_MESSAGE)) {
            _binding.includes.textViewErrorMessage.setVisibility(View.VISIBLE);
            _binding.includes.textViewErrorMessage.setText(getIntent().getStringExtra(EXTRA_KEY_ERROR_MESSAGE));
        }

        switch (itemType) {
            case RegularV1:

                // Set the correct layout visible.
                _binding.includes.includeRegularItem.regularShareItemScrollView.setVisibility(View.VISIBLE);
                _binding.includes.includeResponseItem.responseShareItemScrollView.setVisibility(View.GONE);

                // Set up the completion view where the user selects friends and groups to share
                // with.
                setupCompletionView();

                // Populate the completion view from extras.
                if (getIntent().hasExtra(EXTRA_KEY_RECIPIENTS)) {
                    HashMap<String, String> guidMap = (HashMap<String, String>) getIntent()
                            .getSerializableExtra(EXTRA_KEY_RECIPIENTS);
                    for (String guid : guidMap.keySet()) {
                        _binding.includes.includeRegularItem.destinationCompletionView.addObjectSync(
                                new ShareTarget(guidMap.get(guid), guid, null));
                    }
                }

                // Title is mandatory, URL, message, and attachment are optional.
                _binding.includes.includeRegularItem.editTextItemTitle.setText(getIntent().getStringExtra(EXTRA_KEY_TITLE));
                if (getIntent().hasExtra(EXTRA_KEY_URL)) {
                    _binding.includes.includeRegularItem.editTextItemUrl.setText(getIntent().getStringExtra(EXTRA_KEY_URL));
                }
                if (getIntent().hasExtra(EXTRA_KEY_MESSAGE)) {
                    _binding.includes.includeRegularItem.editTextItemMessage.setText(getIntent().getStringExtra(EXTRA_KEY_MESSAGE));
                }
                if (getIntent().hasExtra(EXTRA_KEY_ATTACHMENT_URI)) {
                    requestReadExternalStoragePermission();
                }

                break;
            case ResponseV1:

                // Set the correct layout visible.
                _binding.includes.includeRegularItem.regularShareItemScrollView.setVisibility(View.GONE);
                _binding.includes.includeResponseItem.responseShareItemScrollView.setVisibility(View.VISIBLE);

                // Response is mandatory.
                _binding.includes.includeResponseItem.editTextItemResponse.setText(getIntent().getStringExtra(EXTRA_KEY_RESPONSE));

                break;
        }
    }

    private void requestReadExternalStoragePermission() {
        // Need read external storage permission.
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                // Have permission.
                _selectedAttachmentUri =
                        Uri.parse(getIntent().getStringExtra(EXTRA_KEY_ATTACHMENT_URI));
                _mimeType = getIntent().getStringExtra(EXTRA_KEY_MIME_TYPE);
                _binding.includes.includeRegularItem.editTextAttachment.setText(getFileName(_selectedAttachmentUri));
                _binding.includes.includeRegularItem.imageButtonRemoveAttachment.setVisibility(View.VISIBLE);
                _binding.includes.includeRegularItem.imageButtonAttachFile.setVisibility(View.GONE);
            } else {
                // Do not have permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void setupCompletionView() {

        // Set up the completion view. Add groups and contacts to it.

        List<ShareTarget> shareTargets = new ArrayList<>();

        Result<Group> groups = _entityStore.select(Group.class)
                .where(Group.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .get();
        for (Group group : groups) {
            shareTargets.add(new ShareTarget(group.getName(), null, group.getId()));
        }

        Result<Friend> friends = _entityStore.select(Friend.class)
                .where(Friend.USER_ACCOUNT_ID.eq(_prefsUtil.getCurrentUserAccountId()))
                .get();
        for (Friend friend : friends) {
            shareTargets.add(new ShareTarget(friend.getName(), friend.getGuid(), null));
        }
        FilteredArrayAdapter<ShareTarget> destinationAdapter =
                new FilteredArrayAdapter<ShareTarget>(this,
                        android.R.layout.simple_list_item_1,
                        shareTargets) {
                    @Override
                    protected boolean keepObject(ShareTarget obj, String mask) {
                        mask = mask.toLowerCase();
                        boolean result = obj.getName().toLowerCase().startsWith(mask);
                        if (obj.getGuid() != null) {
                            result |= obj.getGuid().toLowerCase().startsWith(mask);
                        }
                        return result;
                    }
                };
        _binding.includes.includeRegularItem.destinationCompletionView.setAdapter(destinationAdapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the selected image URI is set, save it to state.
        if (_selectedAttachmentUri != null) {
            outState.putParcelable(STATE_KEY_SELECTED_ATTACHMENT_URI, _selectedAttachmentUri);
        }
        if (_mimeType != null) {
            outState.putString(STATE_KEY_SELECTED_MIME_TYPE, _mimeType);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // The delete menu item should only be visible for items with error messages.
        MenuItem deleteMenuItem = menu.findItem(R.id.action_delete_share);
        deleteMenuItem.setVisible(getIntent().hasExtra(EXTRA_KEY_ERROR_MESSAGE));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share_item, menu);
        return true;
    }

    public void showHelp(MenuItem menuItem) {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_KEY_RAW_RESOURCE_ID, R.raw.help_activity_share_item);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void attachFile(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_SELECT_ATTACHMENT);
    }

    public void removeAttachment(View view) {
        _binding.includes.includeRegularItem.editTextAttachment.setText("");
        _selectedAttachmentUri = null;
        _mimeType = null;
        _binding.includes.includeRegularItem.imageButtonRemoveAttachment.setVisibility(View.GONE);
        _binding.includes.includeRegularItem.imageButtonAttachFile.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _selectedAttachmentUri =
                            Uri.parse(getIntent().getStringExtra(EXTRA_KEY_ATTACHMENT_URI));
                    _mimeType = getIntent().getStringExtra(EXTRA_KEY_MIME_TYPE);
                    _binding.includes.includeRegularItem.editTextAttachment.setText(getFileName(_selectedAttachmentUri));
                    _binding.includes.includeRegularItem.imageButtonRemoveAttachment.setVisibility(View.VISIBLE);
                    _binding.includes.includeRegularItem.imageButtonAttachFile.setVisibility(View.GONE);
                } else {
                    requestReadExternalStoragePermission();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_ATTACHMENT && resultCode == RESULT_OK && data != null) {

            // Ensure that the attachment is something that we can handle.
            boolean canHandleAttachment = false;
            try {
                canHandleAttachment = _attachmentHandlingService.canHandle(data);
            } catch (FileNotFoundException e) {
                Snackbar.make(_binding.layoutCoordinator,
                        R.string.share_item_snack_attachment_file_not_found,
                        Snackbar.LENGTH_LONG).show();
            } catch (IOException e) {
                Snackbar.make(_binding.layoutCoordinator,
                        R.string.share_item_snack_attachment_content_type_error,
                        Snackbar.LENGTH_LONG).show();
            }

            if (canHandleAttachment) {
                _selectedAttachmentUri = data.getData();

                try {
                    _mimeType = _attachmentHandlingService.mimeType(data);
                } catch (IOException e) {
                    // Ignore. This will have been handled above after the canHandle() call.
                }

                _binding.includes.includeRegularItem.editTextAttachment.setText(getFileName(data.getData()));
                _binding.includes.includeRegularItem.imageButtonRemoveAttachment.setVisibility(View.VISIBLE);
                _binding.includes.includeRegularItem.imageButtonAttachFile.setVisibility(View.GONE);
            } else {
                Snackbar.make(_binding.layoutCoordinator,
                        R.string.share_item_snack_attachment_unhandled_file_type,
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void deleteShare(MenuItem item) {
        new AlertDialog.Builder(this).setMessage("Are you sure you want to discard this item?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // Remove the entry from the database and finish the activity.
                    long itemId = getIntent().getLongExtra(EXTRA_KEY_ITEM_ID, 0L);
                    _shareItemService.deleteQueuedItem(itemId);
                    _eventBus.post(new QueuedItemDeleted());
                    setResult(RESULT_DELETED);
                    finish();
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // Noop.
                })
                .show();
    }

    public void submit(MenuItem item) {

        ShareType itemType = (ShareType) getIntent().getSerializableExtra(EXTRA_KEY_ITEM_TYPE);

        // If the item type could not be pulled from an extra, the extra doesn't exist, which means
        // that this is a _new_ regular item.
        if (itemType == null) {
            itemType = ShareType.RegularV1;
        }

        switch (itemType) {
            case RegularV1: {

                // Assemble a list of share targets from the widget. Some are groups and some are
                // friends, so turn that into a list of friends, removing duplicates and stuff.
                Set<String> sharedWithGUIDs =
                        buildSharedWithGUIDs(_binding.includes.includeRegularItem.destinationCompletionView.getObjects());

                // Validation: Ensure that we are sharing with at least one person.
                if (sharedWithGUIDs.isEmpty()) {
                    Snackbar.make(_binding.layoutCoordinator,
                            R.string.share_item_snack_enter_some_share_targets,
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Validation: Ensure a title is entered.
                String title = null;
                if (_binding.includes.includeRegularItem.editTextItemTitle.getText() != null) {
                    title = _binding.includes.includeRegularItem.editTextItemTitle.getText().toString();
                }
                title = StringUtil.trimToNull(title);
                if (title == null) {
                    Snackbar.make(_binding.layoutCoordinator,
                            R.string.share_item_snack_enter_title,
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Validation: There must be a URL, attachment, or message at a minimum. There may
                // be more than one, but there must be at least one.
                String url = null;
                if (_binding.includes.includeRegularItem.editTextItemUrl.getText() != null) {
                    url = _binding.includes.includeRegularItem.editTextItemUrl.getText().toString();
                }
                url = StringUtil.trimToNull(url);
                String message = null;
                if (_binding.includes.includeRegularItem.editTextItemMessage.getText() != null) {
                    message = _binding.includes.includeRegularItem.editTextItemMessage.getText().toString();
                }
                message = StringUtil.trimToNull(message);
                boolean attachmentPresent = _selectedAttachmentUri != null;
                if (url == null && message == null && !attachmentPresent) {
                    Snackbar.make(_binding.layoutCoordinator,
                            R.string.share_item_snack_enter_something_dammit,
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Validation has passed, so put the new item in the database submit it
                // asynchronously, and finish the activity.

                // If the item currently exists, delete it from the database.
                if (getIntent().hasExtra(EXTRA_KEY_ITEM_ID)) {
                    long itemId = getIntent().getLongExtra(EXTRA_KEY_ITEM_ID, 0L);
                    _shareItemService.deleteQueuedItem(itemId);
                    _eventBus.post(new QueuedItemDeleted());
                }

                // Create a new queued item.
                long itemId = _shareItemService.queueItem(_prefsUtil.getCurrentUserAccountId(),
                        sharedWithGUIDs,
                        title,
                        url,
                        message,
                        _selectedAttachmentUri,
                        _mimeType);

                // Let the user know that shit's going down.
                Toast.makeText(this, R.string.share_item_toast_item_queued, Toast.LENGTH_LONG)
                        .show();

                // Submit the item to the server asynchronously,
                _shareItemService.submitItemAsync(MainActivity.class,
                        itemId,
                        _prefsUtil.getCurrentHashedPassword());

                break;
            }
            case ResponseV1: {

                // Validation: Ensure a response has been entered.
                String response = null;
                if (_binding.includes.includeResponseItem.editTextItemResponse.getText() != null) {
                    response = _binding.includes.includeResponseItem.editTextItemResponse.getText().toString();
                }
                response = StringUtil.trimToNull(response);
                if (response == null) {
                    Snackbar.make(_binding.layoutCoordinator,
                            R.string.share_item_snack_enter_response,
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Validation has passed.

                // If the item currently exists, delete it from the database.
                if (getIntent().hasExtra(EXTRA_KEY_ITEM_ID)) {
                    long itemId = getIntent().getLongExtra(EXTRA_KEY_ITEM_ID, 0L);
                    _shareItemService.deleteQueuedItem(itemId);
                    _eventBus.post(new QueuedItemDeleted());
                }

                // Create a new queued item.
                long parentItemRid = getIntent().getLongExtra(EXTRA_KEY_PARENT_ITEM_REMOTE_ID, 0L);
                long rootItemRid = getIntent().getLongExtra(EXTRA_KEY_ROOT_ITEM_REMOTE_ID, 0L);
                long itemId = _shareItemService.queueItem(_prefsUtil.getCurrentUserAccountId(),
                        parentItemRid,
                        rootItemRid,
                        response);

                // Submit the item to the server asynchronously,
                _shareItemService.submitItemAsync(NewsItemDetailActivity.class,
                        itemId,
                        _prefsUtil.getCurrentHashedPassword());

                break;
            }
        }

        setResult(RESULT_SUBMITTED);
        finish();
    }

    /**
     * Convert a list of share targets to a set of guid strings.
     *
     * @param shareTargets
     * @return
     */
    protected Set<String> buildSharedWithGUIDs(List<ShareTarget> shareTargets) {
        Set<String> sharedWithGUIDs = new HashSet<>();
        for (ShareTarget shareTarget : shareTargets) {
            if (shareTarget.getGuid() != null) {
                sharedWithGUIDs.add(shareTarget.getGuid());
            } else if (shareTarget.getGroupId() != null) {
                Result<GroupMember> groupMembers = _entityStore.select(GroupMember.class)
                        .where(GroupMember.GROUP_ID.eq(shareTarget.getGroupId()))
                        .get();
                for (GroupMember groupMember : groupMembers) {
                    sharedWithGUIDs.add(groupMember.getFriend().getGuid());
                }
            }
        }
        return sharedWithGUIDs;
    }
}
