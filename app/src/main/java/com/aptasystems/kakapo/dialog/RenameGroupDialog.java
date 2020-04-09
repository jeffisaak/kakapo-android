package com.aptasystems.kakapo.dialog;

import android.os.Bundle;
import android.view.View;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.entities.Group;
import com.aptasystems.kakapo.event.GroupRenamed;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import kakapo.util.StringUtil;

public class RenameGroupDialog extends BaseDialog {

    private static final String TAG = RenameGroupDialog.class.getSimpleName();

    private static final String ARG_GROUP_ID = "groupId";

    private TextInputLayout _groupNameTextInputLayout;
    private TextInputEditText _groupNameEditText;

    private long _groupId;

    public static RenameGroupDialog newInstance(Long groupId) {
        RenameGroupDialog result = new RenameGroupDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_GROUP_ID, groupId);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _groupId = getArguments().getLong(ARG_GROUP_ID);

        _groupNameTextInputLayout = view.findViewById(R.id.text_input_layout_group_name);
        _groupNameEditText = view.findViewById(R.id.edit_text_group_name);
    }

    @Override
    protected void okPressed() {

        // Perform validation.
        boolean valid = true;
        String groupName = StringUtil.trimToNull(_groupNameEditText.getText().toString());
        if (groupName == null) {
            _groupNameTextInputLayout.setError(getString(R.string.dialog_rename_group_error_create_no_name));
            valid = false;
        }
        if (!valid) {
            return;
        }

        // Clear the errors.
        _groupNameTextInputLayout.setError(null);

        // Rename the group.
        _groupDAO.updateName(_groupId, groupName);

        // Post an event indicating the group was added.
        _eventBus.post(new GroupRenamed(_groupId, groupName));

        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_rename_group;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
