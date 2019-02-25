package com.aloha.kimage.loader;

import java.lang.ref.WeakReference;

import io.flutter.plugin.common.MethodChannel;

public class LoadImageCallback implements Runnable {
    private final WeakReference<MethodChannel.Result> result;
    private final String cacheKey;

    public LoadImageCallback(String cacheKey, MethodChannel.Result result) {
        this.result = new WeakReference<>(result);
        this.cacheKey = cacheKey;
    }

    @Override
    public void run() {
        byte[] data = ImageLoaderJava.instance.getFromCache(cacheKey);
        if (data != null) result.get().success(data);
    }
}
