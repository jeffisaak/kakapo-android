package com.aptasystems.kakapo;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.aptasystems.kakapo.databinding.ActivityViewImageBinding;
import com.aptasystems.kakapo.event.AttachmentDecryptComplete;
import com.aptasystems.kakapo.event.ContentStreamComplete;
import com.aptasystems.kakapo.event.ContentStreamProgress;
import com.aptasystems.kakapo.exception.AsyncResult;
import com.aptasystems.kakapo.service.ShareService;
import com.aptasystems.kakapo.service.TemporaryFileService;
import com.aptasystems.kakapo.util.PrefsUtil;
import com.aptasystems.kakapo.viewmodel.ViewImageModel;
import com.davemorrissey.labs.subscaleview.ImageSource;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class ViewImageActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_REMOTE_ID = "itemRemoteId";

    private static final int PROGRESS_BAR_MAX = 1000;

    @Inject
    EventBus _eventBus;

    @Inject
    ShareService _shareItemService;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    TemporaryFileService _temporaryFileService;

    private CompositeDisposable _compositeDisposable = new CompositeDisposable();
    private ActivityViewImageBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KakapoApplication) getApplication()).getKakapoComponent().inject(this);

        _binding = ActivityViewImageBinding.inflate(getLayoutInflater());

        setContentView(_binding.getRoot());
        setSupportActionBar(_binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Hide the status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Bring the app bar layout to the front.
        _binding.toolbarLayout.bringToFront();

        // Set up the onclick for the image so that we can show/hide the toolbar.
        _binding.includes.photoViewImage.setOnClickListener(v -> toggleUiVisibility());

        // Set up our view model.
        final ViewImageModel viewModel = new ViewModelProvider(this)
                .get(ViewImageModel.class);

        // Observe changes in the displayed filename and update the UI.
        viewModel.getDisplayedFilenameLiveData().observe(this, string -> {
            if (string == null) {
                _binding.includes.layoutProgressIndicator.setVisibility(View.VISIBLE);
            } else {
                File file = new File(string);
                _binding.includes.layoutProgressIndicator.setVisibility(View.GONE);
                _binding.includes.photoViewImage.setImage(ImageSource.uri(Uri.fromFile(file)));
            }
        });

        // Observe changes in the download progress.
        viewModel.getDownloadProgressLiveData().observe(this, progress -> {
            _binding.includes.progressBarImageDecrypt.setMax(PROGRESS_BAR_MAX);
            _binding.includes.progressBarImageDecrypt.setProgress((int) (progress * PROGRESS_BAR_MAX));
        });

        // If the displayed filename hasn't been set, go fetch the data.
        if (viewModel.getDisplayedFilenameLiveData().getValue() == null) {
            _binding.includes.layoutProgressIndicator.setVisibility(View.VISIBLE);

            long itemRid = getIntent().getLongExtra(EXTRA_ITEM_REMOTE_ID, 0L);
            Disposable disposable =
                    _shareItemService.streamItemContentAsync(_prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentHashedPassword(),
                            itemRid);
            _compositeDisposable.add(disposable);
        }
    }

    private void toggleUiVisibility() {
        int systemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
        if ((systemUiVisibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) {
            showSystemUI();
            getSupportActionBar().show();
        } else {
            hideSystemUI();
            getSupportActionBar().hide();
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
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
        _compositeDisposable.clear();
        super.onDestroy();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ContentStreamProgress event) {
        final ViewImageModel viewModel = new ViewModelProvider(this)
                .get(ViewImageModel.class);
        viewModel.getDownloadProgressLiveData().setValue(event.getPercentComplete());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ContentStreamComplete event) {

        if (event.getStatus() == AsyncResult.Success) {
            _binding.includes.textViewProgressDescriptor.setText(getString(R.string.view_image_text_decrypting));

            // We have the content but it's still encrypted.
            Disposable disposable =
                    _shareItemService.decryptAttachmentAsync(_prefsUtil.getCurrentUserAccountId(),
                            _prefsUtil.getCurrentHashedPassword(),
                            event.getEncryptedContent());
            _compositeDisposable.add(disposable);
        } else {

            // FUTURE: We _could_ make this error message a lot more specific by interrogating the status of the event, but this is probably fine for now. Possible statuses here are IncorrectPassword, BadRequest, ContentStreamFailed, Unauthorized, TooManyRequests, OtherHttpError, ServerUnavailable, and RetrofitIOException
            // Show a toast and finish the activity.
            Toast.makeText(this,
                    R.string.view_image_toast_content_stream_failed,
                    Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AttachmentDecryptComplete event) {

        if (event.getStatus() == AsyncResult.Success) {

            // Write the decrypted content to a temporary file.
            File backupFile = _temporaryFileService.newTempFile();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(backupFile);
                fileOutputStream.write(event.getDecryptedContent());
                fileOutputStream.close();
            } catch (IOException e) {
                // Show a toast and finish the activity.
                Toast.makeText(this,
                        R.string.view_image_toast_content_decryption_failed,
                        Toast.LENGTH_LONG).show();
                finishAndRemoveTask();
            }

            final ViewImageModel viewModel = new ViewModelProvider(this)
                    .get(ViewImageModel.class);
            viewModel.getDisplayedFilenameLiveData().setValue(backupFile.getAbsolutePath());

        } else {

            // FUTURE: As above, we can make this error message more specific by checking what the event status is. Possible statuses are DecryptionFailed and ItemDeserializationFailed.
            // Show a toast and finish the activity.
            Toast.makeText(this,
                    R.string.view_image_toast_content_decryption_failed,
                    Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }
    }
}
