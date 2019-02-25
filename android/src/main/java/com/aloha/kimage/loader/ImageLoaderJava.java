package com.aloha.kimage.loader;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ImageLoaderJava {
    public static ImageLoaderJava instance = new ImageLoaderJava();
    public static final int MEDIA_DIR_IMAGE = 0;
    public static final int MEDIA_DIR_VIDEO = 2;
    public static final int MEDIA_DIR_AUDIO = 3;
    private static final int timeoutSeconds = 5;
    private static final int retryCountLimit = 3;

    Map<String, Integer> retryCounts = new HashMap<>();
    private LruCache memCache;
    private ExecutorService thumbGeneratingQueue = Executors.newFixedThreadPool(3);
    public Map<String, ThumbGenerateTaskJava> thumbGenerateTasks = new HashMap<>();

    public void putInCache(String key, byte[] data) {
        memCache.put(key, data);
    }

    public byte[] getFromCache(String key) {
        return memCache.get(key);
    }

    public Bitmap loadBitmap(String path, float maxWidth, float maxHeight, boolean useMaxScale) {
        if (path == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(path, options);

        float photoW = options.outWidth;
        float photoH = options.outHeight;
        maxWidth = maxWidth > 0 ? maxWidth : 1;
        maxHeight = maxHeight > 0 ? maxHeight : 1;
        float scaleFactor = useMaxScale
                ? Math.max(photoW / maxWidth, photoH / maxHeight)
                : Math.min(photoW / maxWidth, photoH / maxHeight);
        if (scaleFactor < 1) scaleFactor = 1f;

        options.inJustDecodeBounds = false;
        options.inSampleSize = (int) scaleFactor;
        if (options.inSampleSize % 2 != 0) {
            int sample = 1;
            while (sample * 2 < options.inSampleSize) {
                sample *= 2;
            }
            options.inSampleSize = sample;
        }
        options.inPurgeable = false;

        Bitmap b = null;

        try {
            b = BitmapFactory.decodeFile(path, options);
        } catch (Throwable e) {
            e.printStackTrace();
            clearMemory();
            try {
                if (b == null) b = BitmapFactory.decodeFile(path, options);
            } catch (Throwable e2) {
                e2.printStackTrace();
            }

        }

        return b;
    }

    public ImageLoaderJava() {
        int cacheSize = Math.min(15, ((ActivityManager) AndroidUtilities.context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() / 7) * 1024 * 1024;
        memCache = new LruCache(cacheSize) {
            @Override
            protected int sizeOf(String key, byte[] value) {
                return value.length;
            }
        };
    }

    public void clearMemory() {
        memCache.evictAll();
    }

    public byte[] generateWebThumbnail(int mediaType, String absolutePath, String name, int width, int height, Runnable doneCallback) {
        byte[] cache = memCache.get(name);
        if (cache != null) return cache;
        ThumbGenerateTaskJava task = thumbGenerateTasks.get(name);
        if (task == null) {
            task = new ThumbGenerateTaskJava(mediaType, absolutePath, name, width, height, doneCallback);
            thumbGenerateTasks.put(name, task);
            thumbGeneratingQueue.submit(task);
            cache = memCache.get(name);
            if (cache != null) return cache;
        } else {
            Integer retryCount = retryCounts.get(name);
            retryCount = retryCount != null ? retryCount : 1;
            if (task.startTime > TimeUnit.SECONDS.toMillis(timeoutSeconds)) {
                task.removeTask();
                retryCounts.put(name, retryCount);
                if (retryCount < retryCountLimit) {
                    task = new ThumbGenerateTaskJava(mediaType, absolutePath, name, width, height, doneCallback);
                    thumbGenerateTasks.put(name, task);
                    thumbGeneratingQueue.submit(task);
                    cache = memCache.get(name);
                    if (cache != null) return cache;
                }
            }
        }

        return null;
    }
}
