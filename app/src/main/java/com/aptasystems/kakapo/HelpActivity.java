package com.aptasystems.kakapo;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import butterknife.BindView;
import butterknife.ButterKnife;

public class HelpActivity extends AppCompatActivity {

    private static final String TAG = HelpActivity.class.getSimpleName();

    private static final String HELP_URL_PREFIX = "com.aptasystems.kakapo://help/";

    private static final String STATE_KEY_HELP_HISTORY = "helpHistory";

    public static final String EXTRA_KEY_RAW_RESOURCE_ID = "rawResourceId";

    private Stack<HelpHistoryEntry> _helpHistory;

    @BindView(R.id.layout_coordinator)
    CoordinatorLayout _coordinatorLayout;

    @BindView(R.id.scroll_view_help_container)
    ScrollView _containerScrollView;

    @BindView(R.id.layout_help_container)
    LinearLayout _containerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        setContentView(R.layout.activity_help);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_HELP_HISTORY)) {

            // Restore from state if possible.
            _helpHistory = (Stack<HelpHistoryEntry>) savedInstanceState.getSerializable(STATE_KEY_HELP_HISTORY);

            // Populate the activity.
            repopulate(_helpHistory.peek()._rawResourceId, _helpHistory.peek()._scrollPosition);

        } else if (_helpHistory != null) {

            // Restore from private member if available.
            // Populate the activity.
            repopulate(_helpHistory.peek()._rawResourceId, _helpHistory.peek()._scrollPosition);

        } else {

            // Try to get the help resource id from the intent. If unavailable, we're coming into
            // the help from a URL. Parse the URL to get the raw help resource id.
            Integer rawHelpResourceId;
            if (getIntent().hasExtra(EXTRA_KEY_RAW_RESOURCE_ID)) {
                rawHelpResourceId = getIntent().getIntExtra(EXTRA_KEY_RAW_RESOURCE_ID, 0);
            } else {
                Uri data = getIntent().getData();
                String rawResourceName = data.getLastPathSegment();
                rawHelpResourceId = getResources().getIdentifier(rawResourceName, "raw", getPackageName());
            }

            // Initialise a new help history stack and push the first entry.
            _helpHistory = new Stack<>();
            HelpHistoryEntry entry = new HelpHistoryEntry();
            entry._rawResourceId = rawHelpResourceId;
            _helpHistory.push(entry);

            // Populate the activity.
            repopulate(entry._rawResourceId, 0);

        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current scroll position and help history.
        _helpHistory.peek()._scrollPosition = _containerScrollView.getScrollY();
        outState.putSerializable(STATE_KEY_HELP_HISTORY, _helpHistory);
    }

    @SuppressLint("InflateParams")
    private void repopulate(int rawHelpResourceId, final int scrollPosition) {

        // Remove old views.
        _containerLayout.removeAllViews();

        // Read the resource.
        InputStream inputStream = getResources().openRawResource(rawHelpResourceId);
        String htmlContent = null;
        try {
            htmlContent = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
        } catch (IOException e) {
            // Show a _toast_ (as we want it to persist across activity changes) and finish this
            // activity.
            Toast.makeText(this, R.string.app_toast_help_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            // Ignore.
        }

        // Parse the content with Jsoup and then turn the HTML into android views.
        // Yes, I know we could just stick the HTML into a web view, and that works okay, but it just
        // doesn't look nice. Native Android views just look nicer and fit in with the rest of the app
        // much better.
        Document document = Jsoup.parse(htmlContent);
        setTitle(document.select("title").text().trim());
        Element body = document.select("body").first();
        Elements children = body.children();
        for (Element child : children) {
            TextView textView = null;
            LinearLayout imageContainerView = null;
            if (child.tagName().compareTo("h1") == 0) {
                // <h1> gets turned into a help_element_h1.
                textView = (TextView) getLayoutInflater().inflate(R.layout.help_element_h1, null);
            } else if (child.tagName().compareTo("h2") == 0) {
                // <h2> gets turned into a help_element_h2.
                textView = (TextView) getLayoutInflater().inflate(R.layout.help_element_h2, null);
            } else if (child.tagName().compareTo("p") == 0) {
                // <p> gets turned into a help_element_content.
                textView = (TextView) getLayoutInflater().inflate(R.layout.help_element_content, null);
            } else if (child.tagName().compareTo("img") == 0) {
                // <img> gets turned into a help_element_image.
                imageContainerView = (LinearLayout) getLayoutInflater().inflate(R.layout.help_element_image, null);
            }

            // Populate the text view or image view if they aren't null. This means that anything that isn't
            // translated from HTML to inflated views is ignored. Good behaviour.
            if (textView != null) {
                textView.setText(Html.fromHtml(child.html().trim()));
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                fixTextView(textView);
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                _containerLayout.addView(textView);
            }
            if (imageContainerView != null) {
                ImageView imageView = imageContainerView.findViewById(R.id.image_view_help_image);
                String imageResourceName = child.attr("src");
                int imageResourceId = getResources().getIdentifier(imageResourceName, "raw", getPackageName());
                InputStream imageInputStream = getResources().openRawResource(imageResourceId);
                imageView.setImageBitmap(BitmapFactory.decodeStream(imageInputStream));
//                imageView.setLayoutParams(new ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT));
//                imageContainerView.invalidate();
//                imageContainerView.requestLayout();
                _containerLayout.addView(imageContainerView);

            }
        }

        // Invalidate the options menu.
        invalidateOptionsMenu();

        // Scroll to where we were/should be.
        _containerScrollView.post(() -> _containerScrollView.scrollTo(0, scrollPosition));
    }

    private void fixTextView(TextView tv) {
        SpannableString current = (SpannableString) tv.getText();
        URLSpan[] spans =
                current.getSpans(0, current.length(), URLSpan.class);

        for (URLSpan span : spans) {
            int start = current.getSpanStart(span);
            int end = current.getSpanEnd(span);

            current.removeSpan(span);
            current.setSpan(new DefensiveURLSpan(span.getURL()), start, end, 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        HelpHistoryEntry currentHistoryEntry = _helpHistory.peek();

        // Help home button visibility id dependent on this page not being home.
        menu.findItem(R.id.action_help_home).setVisible(currentHistoryEntry._rawResourceId != R.raw.help_home);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (_helpHistory.size() == 1) {
            // If we're at the first page, return to the last activity.
            super.onBackPressed();
        } else {
            // Otherwise, pop a record from the stack and repopulate the UI.
            _helpHistory.pop();
            HelpHistoryEntry current = _helpHistory.peek();
            repopulate(current._rawResourceId, current._scrollPosition);
        }
    }

    public void helpHome(MenuItem menuItem) {
        // Save the current scroll position so we can return to it.
        _helpHistory.peek()._scrollPosition = _containerScrollView.getScrollY();

        // Decipher the raw help resource id from the URL.
        int rawHelpResourceId = getResources().getIdentifier("help_home", "raw", getPackageName());

        // Create a new help history record and push it onto the stack.
        HelpHistoryEntry entry = new HelpHistoryEntry();
        entry._rawResourceId = rawHelpResourceId;
        _helpHistory.push(entry);

        // Repopulate the UI.
        repopulate(rawHelpResourceId, 0);
    }

    // This code partially taken from somewhere. Where? Don't remember.
    public class DefensiveURLSpan extends URLSpan {
        public final Creator<DefensiveURLSpan> CREATOR =
                new Creator<DefensiveURLSpan>() {

                    @Override
                    public DefensiveURLSpan createFromParcel(Parcel source) {
                        return new DefensiveURLSpan(source.readString());
                    }

                    @Override
                    public DefensiveURLSpan[] newArray(int size) {
                        return new DefensiveURLSpan[size];
                    }

                };

        private String mUrl;

        public DefensiveURLSpan(String url) {
            super(url);
            mUrl = url;
        }

        @Override
        public void onClick(View widget) {

            if (mUrl.startsWith(HELP_URL_PREFIX)) {

                // Save the current scroll position so we can return to it.
                _helpHistory.peek()._scrollPosition = _containerScrollView.getScrollY();

                // Decipher the raw help resource id from the URL.
                String rawResourceName = mUrl.substring(mUrl.lastIndexOf('/') + 1);
                int rawHelpResourceId = getResources().getIdentifier(rawResourceName, "raw", getPackageName());

                // Create a new help history record and push it onto the stack.
                HelpHistoryEntry entry = new HelpHistoryEntry();
                entry._rawResourceId = rawHelpResourceId;
                _helpHistory.push(entry);

                // Repopulate the UI.
                repopulate(rawHelpResourceId, 0);

            } else {
                super.onClick(widget);
            }
        }
    }

    private static class HelpHistoryEntry implements Serializable {
        private int _rawResourceId;
        private int _scrollPosition;
    }
}
