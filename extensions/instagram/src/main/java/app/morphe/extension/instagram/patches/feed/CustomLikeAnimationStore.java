/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.patches.feed;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.crimera.sharedPreference.SharedPref;
import app.morphe.extension.instagram.settings.Settings;
import app.morphe.extension.shared.Logger;

public final class CustomLikeAnimationStore {
    public static final String SELECTION_PREFIX = "CUSTOM_PIKO:";
    public static final String CARRIER_ANIMATION = "RINGS_LIKE_FUTURA";

    private static final String REGISTRY_KEY = "piko_custom_like_animations";
    private static final String ROOT_DIR = "piko_like_animations";
    private static final int MAX_PACKAGE_BYTES = 20 * 1024 * 1024;
    private static final int MAX_ENTRY_BYTES = 12 * 1024 * 1024;
    private static final int MAX_MANIFEST_BYTES = 64 * 1024;

    private CustomLikeAnimationStore() {}

    public static final class CustomAnimation {
        public final String id;
        public final String name;
        public final String animation;

        CustomAnimation(String id, String name, String animation) {
            this.id = id;
            this.name = name;
            this.animation = animation;
        }
    }

    public static final class ImportResult {
        public final boolean success;
        public final String selection;
        public final String message;

        private ImportResult(boolean success, String selection, String message) {
            this.success = success;
            this.selection = selection;
            this.message = message;
        }

        static ImportResult success(String selection) {
            return new ImportResult(true, selection, "Custom like animation imported");
        }

        static ImportResult failure(String message) {
            return new ImportResult(false, null, message);
        }
    }

    public static String selectionFor(String id) {
        return SELECTION_PREFIX + id;
    }

    public static boolean isCustomSelection(String value) {
        return value != null
                && value.startsWith(SELECTION_PREFIX)
                && value.length() > SELECTION_PREFIX.length();
    }

    public static List<CustomAnimation> getAnimations() {
        ArrayList<CustomAnimation> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(SharedPref.getStringPref(REGISTRY_KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String name = item.optString("name", "");
                String animation = item.optString("animation", "");
                if (isValidId(id) && !name.isEmpty() && name.length() <= 64
                        && isSafeRelativePath(animation)) {
                    result.add(new CustomAnimation(id, name, animation));
                }
            }
        } catch (Exception e) {
            Logger.printException(() -> "custom like animation registry read failed", e);
        }
        return result;
    }

    public static ImportResult importFromUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return ImportResult.failure("Invalid custom like animation package");
        }

        File root = new File(context.getFilesDir(), ROOT_DIR);
        File temp = new File(root, ".import-" + System.nanoTime());
        File packageFile = new File(root, ".package-" + System.nanoTime());

        try {
            if (!root.exists() && !root.mkdirs()) {
                return ImportResult.failure("Failed to create animation storage");
            }

            long copied = copyToFile(context, uri, packageFile, MAX_PACKAGE_BYTES);
            if (copied <= 0) {
                return ImportResult.failure("Invalid custom like animation package");
            }

            if (!temp.mkdirs()) {
                return ImportResult.failure("Failed to prepare animation storage");
            }

            JSONObject manifest = null;
            long extractedBytes = 0L;

            try (ZipInputStream zip = new ZipInputStream(new FileInputStream(packageFile))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    String path = entry.getName();
                    if (!isSafeRelativePath(path)) {
                        throw new IllegalArgumentException("Unsafe animation path");
                    }

                    long declared = entry.getSize();
                    if (declared > MAX_ENTRY_BYTES) {
                        throw new IllegalArgumentException("Animation asset is too large");
                    }

                    File output = new File(temp, path);
                    File canonicalRoot = temp.getCanonicalFile();
                    File canonicalOutput = output.getCanonicalFile();
                    if (!canonicalOutput.toPath().startsWith(canonicalRoot.toPath())) {
                        throw new IllegalArgumentException("Unsafe animation path");
                    }

                    File parent = output.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IllegalArgumentException("Failed to extract animation");
                    }

                    try (FileOutputStream out = new FileOutputStream(output)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        long entryBytes = 0L;
                        while ((read = zip.read(buffer)) != -1) {
                            entryBytes += read;
                            extractedBytes += read;
                            if (entryBytes > MAX_ENTRY_BYTES || extractedBytes > MAX_PACKAGE_BYTES) {
                                throw new IllegalArgumentException("Animation package is too large");
                            }
                            out.write(buffer, 0, read);
                        }
                    }

                    if ("manifest.json".equals(path)) {
                        if (output.length() > MAX_MANIFEST_BYTES) {
                            throw new IllegalArgumentException("Animation manifest is too large");
                        }
                        manifest = new JSONObject(
                                new String(
                                        java.nio.file.Files.readAllBytes(output.toPath()),
                                        StandardCharsets.UTF_8
                                )
                        );
                    }
                }
            }

            if (manifest == null || manifest.optInt("format", -1) != 1) {
                throw new IllegalArgumentException("Unsupported animation package format");
            }

            String id = manifest.optString("id", "");
            String name = manifest.optString("name", "");
            String animation = manifest.optString("animation", "");

            if (!isValidId(id)
                    || name.trim().isEmpty()
                    || name.length() > 64
                    || !isSafeRelativePath(animation)
                    || !isSupportedAnimation(animation)) {
                throw new IllegalArgumentException("Invalid custom like animation manifest");
            }

            File animationFile = new File(temp, animation);
            if (!animationFile.isFile()
                    || animationFile.length() <= 0
                    || animationFile.length() > MAX_ENTRY_BYTES) {
                throw new IllegalArgumentException("Animation asset is missing or invalid");
            }

            File destination = new File(root, id);
            deleteRecursively(destination);
            if (!temp.renameTo(destination)) {
                throw new IllegalArgumentException("Failed to install animation package");
            }

            ArrayList<CustomAnimation> animations = new ArrayList<>(getAnimations());
            animations.removeIf(item -> item.id.equals(id));
            animations.add(new CustomAnimation(id, name.trim(), animation));

            JSONArray registry = new JSONArray();
            for (CustomAnimation item : animations) {
                JSONObject object = new JSONObject();
                object.put("id", item.id);
                object.put("name", item.name);
                object.put("animation", item.animation);
                registry.put(object);
            }
            SharedPref.setStringPref(REGISTRY_KEY, registry.toString());

            return ImportResult.success(selectionFor(id));
        } catch (Exception e) {
            deleteRecursively(temp);
            Logger.printException(() -> "custom like animation import failed", e);
            return ImportResult.failure("Failed to import custom like animation");
        } finally {
            if (packageFile.exists()) packageFile.delete();
        }
    }

    public static Drawable createDrawableForCurrentSelection(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 28) return null;

        String selection = SharedPref.getStringPref(
                Settings.CHANGE_LIKE_ANIMATION.key,
                Settings.CHANGE_LIKE_ANIMATION.defaultValue
        );
        if (!isCustomSelection(selection)) return null;

        String id = selection.substring(SELECTION_PREFIX.length());
        CustomAnimation animation = null;
        for (CustomAnimation candidate : getAnimations()) {
            if (candidate.id.equals(id)) {
                animation = candidate;
                break;
            }
        }
        if (animation == null) return null;

        try {
            File file = new File(
                    new File(context.getFilesDir(), ROOT_DIR),
                    id + File.separator + animation.animation
            );
            if (!file.isFile()) return null;

            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file));
            if (drawable instanceof AnimatedImageDrawable) {
                AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
                animated.setRepeatCount(0);
                animated.start();
            }
            return drawable;
        } catch (Exception e) {
            Logger.printException(() -> "custom like animation drawable failed", e);
            return null;
        }
    }

    private static long copyToFile(Context context, Uri uri, File destination, int maxBytes)
            throws Exception {
        long total = 0L;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IllegalArgumentException("Unable to open animation package");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("Animation package is too large");
                }
                output.write(buffer, 0, read);
            }
        }
        return total;
    }

    private static boolean isValidId(String id) {
        return id != null && id.length() >= 1 && id.length() <= 64
                && id.matches("[A-Za-z0-9._-]+");
    }

    private static boolean isSafeRelativePath(String path) {
        return path != null && !path.isEmpty()
                && !path.startsWith("/")
                && !path.startsWith("\\")
                && !path.contains("\\")
                && !path.contains("../")
                && !path.contains("..\\");
    }
    private static boolean isSupportedAnimation(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.endsWith(".png");
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }
}
