package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

/** Makes the four packaged pictures available to the existing wallpaper picker. */
final class BundledWallpapers {
    private static final String[] FILES = {
            "bundled-01-aurora.jpg", "bundled-02-mountain-lake.jpg",
            "bundled-03-cr7.jpg", "bundled-04-avengers.jpg"
    };
    private static final String DEFAULT = "bundled-03-cr7.jpg";
    private BundledWallpapers() { }

    static void install(Context context) {
        File theme = Config.dir(context);
        if (theme == null) return; // Storage may be unavailable; retry on the next Home creation.
        File collection = new File(theme, "wallpapers");
        SharedPreferences choices = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        boolean fresh = choices.getAll().isEmpty() && !new File(theme, Config.FILE_NAME).exists()
                && !new File(theme, Config.WALLPAPER_NAME).exists() && !collection.exists();
        for (String name : FILES) {
            File destination = new File(collection, name);
            if (destination.exists()) continue;
            try (InputStream source = context.getAssets().open("wallpapers/" + name)) {
                copyMissing(source, destination);
            } catch (IOException | SecurityException error) {
                Log.w("BundledWallpapers", "Could not prepare " + name, error);
            }
        }
        // Only select a default on a genuinely fresh install. Existing themes, "none", and user
        // selections survive APK updates. Config.load keeps this choice on its first absent-theme sync.
        if (fresh && new File(collection, DEFAULT).isFile()) {
            if (!choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, DEFAULT).commit()) {
                Log.w("BundledWallpapers", "Could not save initial wallpaper choice");
            }
        }
    }

    /** Publish only a completed copy; a user-supplied existing file is never overwritten. */
    static boolean copyMissing(InputStream source, File target) throws IOException {
        if (target.exists()) return false;
        File parent = target.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())) {
            throw new IOException("Wallpaper directory is unavailable");
        }
        File temporary = File.createTempFile("bundled-wallpaper-", ".tmp", parent);
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[16_384];
                int count;
                while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            try {
                Files.move(temporary.toPath(), target.toPath()); // No REPLACE_EXISTING.
                return true;
            } catch (FileAlreadyExistsException ignored) {
                return false;
            }
        } finally {
            if (temporary.exists() && !temporary.delete()) temporary.deleteOnExit();
        }
    }
}
