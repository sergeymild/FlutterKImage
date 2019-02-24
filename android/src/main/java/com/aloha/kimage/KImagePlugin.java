package com.aloha.kimage;

import android.os.Environment;

import com.aloha.kimage.loader.AndroidUtilities;
import com.aloha.kimage.loader.ImageLoaderJava;

import java.io.File;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

public class KImagePlugin implements MethodChannel.MethodCallHandler {

    public String externalAbsolutePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static void registerWith(PluginRegistry.Registrar registrar) {
        AndroidUtilities.context = registrar.activeContext().getApplicationContext();
        MethodChannel channel = new MethodChannel(registrar.messenger(), "k_image");
        channel.setMethodCallHandler(new KImagePlugin());
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        if (call.method.equals("documentsFolder")) {
            result.success(externalAbsolutePath());
            return;
        }

        String localPath = call.argument("path");
        int width = call.hasArgument("width") ? ((int)call.argument("width")) : -1;
        int height = call.hasArgument("height") ? ((int) call.argument("height")) : -1;

        if (localPath == null) {
            result.error("LocalPath is empty", null, null);
            return;
        }

        File file = new File(localPath);
        if (!file.exists()) {
            result.error("LocalPath is empty", null, null);
            return;
        }

        int type = -1;
        if (call.method.equals("fetchArtworkFromLocalPath")) {
            type = ImageLoaderJava.MEDIA_DIR_AUDIO;

        } else if (call.method.equals("loadImageFromLocalPath")) {
            type = ImageLoaderJava.MEDIA_DIR_IMAGE;
        }

        if (type == -1) {
            result.notImplemented();
            return;
        }

        int index = localPath.lastIndexOf('/');
        String name = localPath.substring(index + 1);
        byte[] data = ImageLoaderJava.instance.generateWebThumbnail(type, localPath, name, width, height);
        if (data != null) result.success(data);
    }
}
