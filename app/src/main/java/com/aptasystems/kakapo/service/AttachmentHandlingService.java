package com.aptasystems.kakapo.service;

import android.content.Context;
import android.content.Intent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AttachmentHandlingService {

    private static final String TAG = AttachmentHandlingService.class.getSimpleName();

    private Context _context;

    @Inject
    public AttachmentHandlingService(Context context) {
        _context = context;
    }

    public boolean canHandle(Intent activityResultIntent) throws IOException {
        String mimeType = mimeType(activityResultIntent);
        return canHandle(mimeType);
    }

    public String mimeType(Intent activityResultIntent) throws IOException {

        // The mime type may be specified in the intent.
        String mimeType = activityResultIntent.getType();

        // Otherwise we will _try_ to guess it from the stream.
        if (mimeType == null) {
            InputStream inputStream = _context.getContentResolver()
                    .openInputStream(activityResultIntent.getData());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            mimeType = URLConnection.guessContentTypeFromStream(bufferedInputStream);
        }

        return mimeType;
    }

    private boolean canHandle(String mimeType) {
       return Objects.equals(mimeType, "image/jpeg") || Objects.equals(mimeType, "image/png");
    }

}
