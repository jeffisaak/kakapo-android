package com.aptasystems.kakapo.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.core.content.FileProvider;

public class ShareUtil {

    private static final String SHARE_BINARY_MIME_TYPE = "application/octet-stream";
    private static final String SHARE_TEXT_MIME_TYPE = "text/plain";

    private ShareUtil() {
        // Prevents instantiation.
    }

    public static Intent buildShareIntent(Context context, String applicationId, File file) {
        List<File> files = new ArrayList<>();
        files.add(file);

        return buildShareIntent(context, applicationId, files);
    }

    public static Intent buildShareIntent(Context context, String applicationId, List<File> files) {

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : files) {
            Uri uri = FileProvider.getUriForFile(context, applicationId + ".provider", file);
            uris.add(uri);
        }
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setType(SHARE_BINARY_MIME_TYPE);
        return shareIntent;
    }

    public static Intent buildShareIntent(String text) {
        Intent result = new Intent();
        result.setAction(Intent.ACTION_SEND);
        result.putExtra(Intent.EXTRA_TEXT, text);
        result.setType(SHARE_TEXT_MIME_TYPE);
        return result;
    }


}
