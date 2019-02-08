/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package com.aloha.kimage.loader

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.view.View
import com.aloha.kimage.loader.VideoThumbnailUtils.createVideoThumbnail
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal class ThumbGenerateTask(private val mediaType: Int, private val absolutePath: String, private var name: String?, var width: Int?, var height: Int?) : Runnable {
    internal var startTime: Long = 0

    internal fun removeTask() {
        name?.let { ImageLoader.thumbGenerateTasks.remove(it) }
    }

    override fun run() {
        try {
            startTime = System.currentTimeMillis()
            if (name == null) {
                removeTask()
                return
            }

            var originalBitmap: Bitmap? = null
            var thumbFile: File? = null
            val cacheDir = AndroidUtilities.getCacheDir()
            if (cacheDir != null) {
                thumbFile = File(cacheDir, name!!)
                if (thumbFile.exists()) {
                    originalBitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
                }
            }

            if (originalBitmap != null) {
            } else if (mediaType == ImageLoader.MEDIA_DIR_IMAGE) {
                originalBitmap = ImageLoader.loadBitmap(absolutePath, width, height, false)
            } else if (mediaType == ImageLoader.MEDIA_DIR_VIDEO) {
                originalBitmap = createVideoThumbnail(absolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
            } else if (mediaType == ImageLoader.MEDIA_DIR_AUDIO) {

                val mediaMetadataRetriever = FFmpegMediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(absolutePath)
                val bytes = mediaMetadataRetriever.embeddedPicture
                if (bytes != null && bytes.isNotEmpty()) {
                    originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }

            }
            if (originalBitmap == null) {
                removeTask()
                return
            }

            val w = originalBitmap.width
            val h = originalBitmap.height
            if (w == 0 || h == 0) {
                removeTask()
                return
            }

            if (width != null && height != null) {
                originalBitmap = SizeTransform.transformResult(originalBitmap, width!!, height!!)
            }

            //originalBitmap = scale(originalBitmap, width, height);

            if (thumbFile?.exists() == true || !File(absolutePath).exists()) {
                removeTask()
                putInCacheAndNotify(originalBitmap)
                return
            }

            thumbFile?.let {
                val stream = FileOutputStream(thumbFile)
                originalBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                try {
                    stream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                putInCacheAndNotify(originalBitmap)
            }

        } catch (e: Throwable) {
            e.printStackTrace()
            removeTask()
        }

    }

    private fun putInCacheAndNotify(originalBitmap: Bitmap?) {
        originalBitmap ?: return
        val outputStream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val bytes = outputStream.toByteArray()
        ImageLoader.memCache.put(name, bytes)
    }
}

object ImageLoader {
    const val MEDIA_DIR_IMAGE = 0
    const val MEDIA_DIR_VIDEO = 2
    const val MEDIA_DIR_AUDIO = 3
    private const val timeoutSeconds = 5
    private const val retryCountLimit = 3

    fun loadBitmap(path: String?, maxWidth: Int?, maxHeight: Int?, useMaxScale: Boolean): Bitmap? {
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true

        if (path != null) BitmapFactory.decodeFile(path, bmOptions)


        val photoW = bmOptions.outWidth.toFloat()
        val photoH = bmOptions.outHeight.toFloat()
        val maxWidth = maxWidth ?: 1
        val maxHeight = maxHeight ?: 1
        var scaleFactor = if (useMaxScale) Math.max(photoW / maxWidth.toFloat(), photoH / maxHeight.toFloat()) else Math.min(photoW / maxWidth.toFloat(), photoH / maxHeight.toFloat())
        if (scaleFactor < 1) {
            scaleFactor = 1f
        }
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor.toInt()
        if (bmOptions.inSampleSize % 2 != 0) {
            var sample = 1
            while (sample * 2 < bmOptions.inSampleSize) {
                sample *= 2
            }
            bmOptions.inSampleSize = sample
        }
        bmOptions.inPurgeable = false

        var b: Bitmap? = null

        try {
            b = BitmapFactory.decodeFile(path, bmOptions)
        } catch (e: Throwable) {
            e.printStackTrace()
            clearMemory()
            try {
                if (b == null) b = BitmapFactory.decodeFile(path, bmOptions)
            } catch (e2: Throwable) {
                e2.printStackTrace()
            }

        }

        return b
    }

    private val retryCounts = HashMap<String, Int>()
    internal val memCache: LruCache

    private val thumbGeneratingQueue = newFixedThreadPoolContext(3, "thumbGeneratingQueue")
    internal val thumbGenerateTasks = HashMap<String, ThumbGenerateTask>()

    init {
        val cacheSize = Math.min(15, (AndroidUtilities.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass / 7) * 1024 * 1024

        memCache = object : LruCache(cacheSize) {
            override fun sizeOf(key: String, value: ByteArray) = value.size
        }
    }

    fun clearMemory() {
        memCache.evictAll()
    }

    suspend fun generateWebThumbnail(mediaType: Int, absolutePath: String, name: String, width: Int?, height: Int?): ByteArray? {
        memCache.get(name)?.let { return it }
        var task: ThumbGenerateTask? = thumbGenerateTasks[name]
        if (task == null) {
            task = ThumbGenerateTask(mediaType, absolutePath, name, width, height)
            thumbGenerateTasks[name] = task
            withContext(thumbGeneratingQueue) { task?.run() }
            memCache.get(name)?.let { return it }
        } else {
            var retryCount: Int? = retryCounts[name]
            retryCount = if (retryCount != null) retryCount + 1 else 1
            Log.i("ImageLoader", "Task exists for: $name -> ${TimeUnit.MILLISECONDS.toSeconds(task.startTime)} seconds $retryCount retryCount")
            if (task.startTime > TimeUnit.SECONDS.toMillis(timeoutSeconds.toLong())) {
                task.removeTask()
                retryCounts[name] = retryCount
                if (retryCount < retryCountLimit) {
                    task = ThumbGenerateTask(mediaType, absolutePath, name, width, height)
                    thumbGenerateTasks[name] = task
                    withContext(thumbGeneratingQueue) { task.run() }
                    memCache.get(name)?.let { return it }
                }
            }
        }
        return null
    }
}
