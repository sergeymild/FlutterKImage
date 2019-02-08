package com.aloha.kimage

import android.os.Environment
import com.aloha.kimage.loader.AndroidUtilities
import com.aloha.kimage.loader.ImageLoader
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class KImagePlugin : MethodCallHandler {
    private val externalAbsolutePath by lazy {
        Environment.getExternalStorageDirectory().absolutePath
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            AndroidUtilities.context = registrar.activeContext().applicationContext
            val channel = MethodChannel(registrar.messenger(), "k_image")
            channel.setMethodCallHandler(KImagePlugin())
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "documentsFolder") {
            result.success(externalAbsolutePath)
            return
        }

        val localPath = call.argument<String>("path")
        val width = call.argument<Int>("width")
        val height = call.argument<Int>("height")

        val file = File(localPath)
        if (localPath == null || !file.exists()) {
            result.error("LocalPath is empty", null, null)
            return
        }

        var type = -1
        if (call.method == "fetchArtworkFromLocalPath") {
            type = ImageLoader.MEDIA_DIR_AUDIO

        } else if (call.method == "loadImageFromLocalPath") {
            type = ImageLoader.MEDIA_DIR_IMAGE
        }

        if (type == -1) {
            result.notImplemented()
            return
        }

        GlobalScope.launch {
            ImageLoader.generateWebThumbnail(type, localPath, localPath.substringAfterLast("/"), width, height)?.let {
                result.success(it)
            }
        }
    }
}
