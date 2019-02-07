package com.aloha.kimage

import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import com.squareup.picasso.Picasso
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.ByteArrayOutputStream
import java.io.File

class KImagePlugin : MethodCallHandler {

    private val handleThread = HandlerThread("KImagePlugin").also {
        it.start()
    }

    private val handler = Handler(handleThread.looper)

    private val externalAbsolutePath by lazy {
        Environment.getExternalStorageDirectory().absolutePath
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val builder = Picasso.Builder(registrar.activeContext().applicationContext)
            builder.addRequestHandler(Mp3Cover())
            Picasso.setSingletonInstance(builder.build())
            val channel = MethodChannel(registrar.messenger(), "k_image")
            channel.setMethodCallHandler(KImagePlugin())
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "documentsFolder") {
            result.success(externalAbsolutePath)
            return
        }

        var localPath = call.argument<String>("path")
        val width = call.argument<Int>("width")
        val height = call.argument<Int>("height")
        val quality = call.argument<Int>("quality")

        if (localPath == null || !File(localPath).exists()) {
            result.error("LocalPath is empty", null, null)
            return
        }

        if (call.method == "fetchArtworkFromLocalPath") {
            localPath = "mp3_artwork_$localPath"
            handler.post {
                loadImage(localPath, width, height, quality, result)
            }

            return
        }

        if (call.method == "loadImageFromLocalPath") {

            handler.post {
                loadImage(localPath, width, height, quality, result)
            }

            return
        }


        result.notImplemented()
    }

    private fun loadImage(localPath: String?, width: Int?, height: Int?, quality: Int?, result: Result) {
        val loader = Picasso.get().load(File(localPath))
        if (width != null && height != null) {
            loader.resize(width, height)
        }
        val bitmap = loader.get()
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality ?: 100, outputStream)
        result.success(outputStream.toByteArray())
    }
}
