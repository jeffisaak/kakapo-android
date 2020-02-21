package com.aptasystems.kakapo.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aptasystems.kakapo.R;
import com.tokenautocomplete.FilteredArrayAdapter;
import com.tokenautocomplete.TokenCompleteTextView;

/**
 * Created by jisaak on 2017-08-14.
 */
public class ShareTargetCompletionView extends TokenCompleteTextView<ShareTarget> {

    private static final String TAG = ShareTargetCompletionView.class.getSimpleName();

    public ShareTargetCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View getViewForObject(ShareTarget shareTarget) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        boolean validDestination = false;
        FilteredArrayAdapter<ShareTarget> adapter = (FilteredArrayAdapter<ShareTarget>) getAdapter();
        for (int ii = 0; ii < adapter.getCount(); ii++) {
            ShareTarget destination = adapter.getItem(ii);
            if (shareTarget.getName().compareTo(destination.getName()) == 0 &&
                    ((shareTarget.getGuid() == null && destination.getGuid() == null)
                            || shareTarget.getGuid().compareTo(destination.getGuid()) == 0)) {
                validDestination = true;
                break;
            }
        }

        TextView view = (TextView) l.inflate(R.layout.token_destination, (ViewGroup) getParent(), false);
        if (!validDestination) {
            view.setBackgroundResource(R.drawable.token_background_error);
        } else {
            view.setBackgroundResource(R.drawable.token_background);
        }
        view.setText(shareTarget.getName());

        return view;
    }

    @Override
    protected ShareTarget defaultObject(String completionText) {

        ShareTarget result = null;

        // Try and match the completion text to a valid name or guid.
        FilteredArrayAdapter<ShareTarget> adapter = (FilteredArrayAdapter<ShareTarget>) getAdapter();
        for (int ii = 0; ii < adapter.getCount(); ii++) {
            ShareTarget destination = adapter.getItem(ii);
            if (completionText.toLowerCase().compareTo(destination.getName().toLowerCase()) == 0 ||
                    (destination.getGuid() != null &&
                            completionText.toLowerCase().compareTo(destination.getGuid().toLowerCase()) == 0)) {
                result = destination;
                break;
            }
        }

        // If we couldn't, just use the name as entered.
        if (result == null) {
            result = new ShareTarget(completionText, null, null);
        }

        return result;
    }
}
