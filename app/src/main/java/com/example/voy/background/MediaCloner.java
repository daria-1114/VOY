package com.example.voy.background;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

public class MediaCloner {
    private static final String TAG = "MediaCloner";
    private static final String FOLDER_NAME = "trip_media";
    public static String cloneToInternal(Context context, Uri source, String extension) {
        File destFile = null;
        try {
            File folder = new File(context.getFilesDir(), FOLDER_NAME);
            if (!folder.exists()) folder.mkdirs();

            String fileName = "media_" + UUID.randomUUID().toString() + extension;
            destFile = new File(folder, fileName);
            try (InputStream is = context.getContentResolver().openInputStream(source);
                 OutputStream os = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }

            return Uri.fromFile(destFile).toString();
        } catch (Exception e) {
            Log.e(TAG, "failed to clone media" + source, e);
            if (destFile.exists()) destFile.delete();
            return null;
        }
    }
}
