package com.aptasystems.kakapo.service;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides file utility.
 */
@Singleton
public class TemporaryFileService {

    private static final String TEMPORARY_FOLDER = "temp";

    private File _tempFolder;

    @Inject
    public TemporaryFileService(Context context) {
        _tempFolder = new File(context.getExternalFilesDir(null), TEMPORARY_FOLDER);
    }

    /**
     * Create a new temporary file in external storage with the given name.  This method does NOT
     * check whether external storage is available.
     *
     * @param name
     * @return
     */
    public File newTempFile(String name) {
        if (!_tempFolder.exists()) {
            _tempFolder.mkdirs();
        }
        return new File(_tempFolder, name);
    }

    /**
     * Create a new temporary file in external storage.  This method does NOT check whether external
     * storage is available.
     *
     * @return
     */
    public File newTempFile() {
        return newTempFile(UUID.randomUUID().toString());
    }

    /**
     * Cleans up all the temporary files.
     */
    public void cleanupAsync() {
        Runnable cleanupRunnable = () -> {
            if (_tempFolder == null || !_tempFolder.exists()) {
                return;
            }
            for (File file : _tempFolder.listFiles()) {
                file.delete();
            }
        };
        new Thread(cleanupRunnable).start();
    }

    /**
     * Get the external storage state.
     *
     * @return
     */
    public StorageState getExternalStorageState() {
        StorageState result = StorageState.NOT_AVAILABLE;

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.compareTo(state) == 0) {
            result = StorageState.WRITABLE;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.compareTo(state) == 0) {
            result = StorageState.READ_ONLY;
        }
        return result;
    }

    public enum StorageState {NOT_AVAILABLE, WRITABLE, READ_ONLY}
}
