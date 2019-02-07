package com.aloha.kimage

import android.graphics.BitmapFactory
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import wseemann.media.FFmpegMediaMetadataRetriever

class Mp3Cover : RequestHandler() {
    override fun canHandleRequest(data: Request?) =
        data?.uri?.path?.startsWith("/mp3_artwork_") == true

    override fun load(request: Request?, networkPolicy: Int): Result? {

        val path = request?.uri?.path?.replace("/mp3_artwork_", "") ?: return null

        val retriever = FFmpegMediaMetadataRetriever()

        try {
            retriever.setDataSource(path)
            val data = retriever.embeddedPicture
            return Result(BitmapFactory.decodeByteArray(data, 0, data.size), Picasso.LoadedFrom.NETWORK)
        } finally {
            retriever.release()
        }
    }

}