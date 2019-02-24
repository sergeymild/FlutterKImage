package com.aloha.kimage.loader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import wseemann.media.FFmpegMediaMetadataRetriever;

import static com.aloha.kimage.loader.VideoThumbnailUtils.createVideoThumbnail;

public class ThumbGenerateTaskJava implements Runnable {
    final int mediaType;
    final String absolutePath;
    String name;
    int width;
    int height;

    long startTime = 0;

    public ThumbGenerateTaskJava(int mediaType, String absolutePath, String name, int width, int height) {
        this.mediaType = mediaType;
        this.absolutePath = absolutePath;
        this.name = name;
        this.width = width;
        this.height = height;
    }

    public void removeTask() {
        if (name == null) return;
        ImageLoaderJava.instance.thumbGenerateTasks.remove(name);
    }

    @Override
    public void run() {
        try {
            startTime = System.currentTimeMillis();
            if (name == null) return;

            Bitmap originalBitmap = null;
            File thumbFile = null;
            File cacheDir = AndroidUtilities.getCacheDir();
            if (cacheDir != null) {
                thumbFile = new File(cacheDir, name);
                if (thumbFile.exists()) {
                    originalBitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
                }
            }

            if (originalBitmap != null) {
            } else if (mediaType == ImageLoaderJava.MEDIA_DIR_IMAGE) {
                originalBitmap = ImageLoaderJava.instance.loadBitmap(absolutePath, width, height, false);
            } else if (mediaType == ImageLoaderJava.MEDIA_DIR_VIDEO) {
                originalBitmap = createVideoThumbnail(absolutePath, MediaStore.Video.Thumbnails.MINI_KIND);
            } else if (mediaType == ImageLoaderJava.MEDIA_DIR_AUDIO) {

                FFmpegMediaMetadataRetriever mediaMetadataRetriever = new FFmpegMediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(absolutePath);
                byte[] bytes = mediaMetadataRetriever.getEmbeddedPicture();
                if (bytes != null && bytes.length > 0) {
                    originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                }
            }

            if (originalBitmap == null) {
                removeTask();
                return;
            }

            int w = originalBitmap.getWidth();
            int h = originalBitmap.getHeight();
            if (w == 0 || h == 0) {
                removeTask();
                return;
            }

            if (width != -1 && height != -1) {
                originalBitmap = SizeTransform.transformResult(originalBitmap, width, height);
            }

            if (thumbFile != null && thumbFile.exists() || !new File(absolutePath).exists()) {
                removeTask();
                putInCacheAndNotify(originalBitmap);
                return;
            }

            if (thumbFile == null) return;

            FileOutputStream stream = new FileOutputStream(thumbFile);
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            try {
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            putInCacheAndNotify(originalBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            removeTask();
        }
    }

    private void putInCacheAndNotify(Bitmap originalBitmap) {
        if (originalBitmap == null) return;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] bytes = outputStream.toByteArray();
        ImageLoaderJava.instance.memCache.put(name, bytes);
    }
}
