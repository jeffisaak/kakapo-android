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
import android.widget.TextView;
import android.widget.Toast;

import com.aptasystems.kakapo.databinding.ActivityHelpBinding;
import com.aptasystems.kakapo.viewmodel.HelpActivityModel;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class HelpActivity extends AppCompatActivity {

    private static final String HELP_URL_PREFIX = "com.aptasystems.kakapo://help/";

    public static final String EXTRA_KEY_RAW_RESOURCE_ID = "rawResourceId";

    private ActivityHelpBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        _binding = ActivityHelpBinding.inflate(getLayoutInflater());

        setContentView(_binding.getRoot());

        setSupportActionBar(_binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final HelpActivityModel viewModel =
                new ViewModelProvider(this).get(HelpActivityModel.class);
        viewModel.getHelpHistoryOpenLiveData().observe(this, helpHistory -> {
            HelpHistoryEntry helpHistoryEntry = helpHistory.peek();
            repopulate(helpHistoryEntry._rawResourceId, helpHistoryEntry._scrollPosition);
        });

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

        // Initialise a new help history stack and push the first entry. This will update the UI
        // thanks to our lovely observer up above.
        // Unfortunately the observer won't get triggered if we just push an entry, we have to update
        // the whole value.  Yuck.
        HelpHistoryEntry entry = new HelpHistoryEntry();
        entry._rawResourceId = rawHelpResourceId;
        Stack<HelpHistoryEntry> historyStack = viewModel.getHelpHistoryOpenLiveData().getValue();
        historyStack.push(entry);
        viewModel.getHelpHistoryOpenLiveData().setValue(historyStack);
    }

    @SuppressLint("InflateParams")
    private void repopulate(int rawHelpResourceId, final int scrollPosition) {

        // Remove old views.
        _binding.includes.layoutHelpContainer.removeAllViews();

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
                _binding.includes.layoutHelpContainer.addView(textView);
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
                _binding.includes.layoutHelpContainer.addView(imageContainerView);

            }
        }

        // Invalidate the options menu.
        invalidateOptionsMenu();

        // Scroll to where we were/should be.
        _binding.includes.getRoot().post(() ->
                _binding.includes.getRoot().scrollTo(0, scrollPosition));
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

        final HelpActivityModel viewModel =
                new ViewModelProvider(this).get(HelpActivityModel.class);
        HelpHistoryEntry currentHistoryEntry = viewModel.getHelpHistoryOpenLiveData().getValue().peek();

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

        final HelpActivityModel viewModel =
                new ViewModelProvider(this).get(HelpActivityModel.class);

        if (viewModel.getHelpHistoryOpenLiveData().getValue().size() == 1) {
            // If we're at the first page, return to the last activity.
            super.onBackPressed();
        } else {
            // Otherwise, pop a record from the stack and repopulate the UI.
            Stack<HelpHistoryEntry> historyStack = viewModel.getHelpHistoryOpenLiveData().getValue();
            historyStack.pop();
            viewModel.getHelpHistoryOpenLiveData().setValue(historyStack);
        }
    }

    public void helpHome(MenuItem menuItem) {

        final HelpActivityModel viewModel =
                new ViewModelProvider(this).get(HelpActivityModel.class);
        Stack<HelpHistoryEntry> historyStack = viewModel.getHelpHistoryOpenLiveData().getValue();

        // Save the current scroll position so we can return to it.
        historyStack.peek()._scrollPosition = _binding.includes.getRoot().getScrollY();

        // Decipher the raw help resource id from the URL.
        int rawHelpResourceId = getResources().getIdentifier("help_home", "raw", getPackageName());

        // Create a new help history record and push it onto the stack.
        HelpHistoryEntry entry = new HelpHistoryEntry();
        entry._rawResourceId = rawHelpResourceId;
        historyStack.push(entry);
        viewModel.getHelpHistoryOpenLiveData().setValue(historyStack);
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

                final HelpActivityModel viewModel =
                        new ViewModelProvider(HelpActivity.this).get(HelpActivityModel.class);
                Stack<HelpHistoryEntry> historyStack = viewModel.getHelpHistoryOpenLiveData().getValue();

                // Save the current scroll position so we can return to it.
                historyStack.peek()._scrollPosition = _binding.includes.getRoot().getScrollY();

                // Decipher the raw help resource id from the URL.
                String rawResourceName = mUrl.substring(mUrl.lastIndexOf('/') + 1);
                int rawHelpResourceId = getResources().getIdentifier(rawResourceName, "raw", getPackageName());

                // Create a new help history record and push it onto the stack.
                HelpHistoryEntry entry = new HelpHistoryEntry();
                entry._rawResourceId = rawHelpResourceId;
                historyStack.push(entry);
                viewModel.getHelpHistoryOpenLiveData().setValue(historyStack);

            } else {
                super.onClick(widget);
            }
        }
    }

    public static class HelpHistoryEntry implements Serializable {
        public int _rawResourceId;
        public int _scrollPosition;
    }
}
