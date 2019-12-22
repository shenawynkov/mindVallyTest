package com.shenawynkov.downloadany

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import okhttp3.Call
import java.io.File
import java.io.IOException

object ImageLoader {
    private fun checkImageInCache(imageUrl: String): Bitmap? = Downloader.memoryCache.get(imageUrl) as Bitmap?
    private fun loadImageIntoImageView(imageView: ImageView, bitmap: Bitmap?) {

        require(bitmap != null) {
            "ImageLoader:loadImageIntoImageView - Bitmap should not be null"
        }

        val scaledBitmap = Utils.scaleBitmapForLoad(bitmap, imageView.width, imageView.height)

        scaledBitmap?.let {
            imageView.setImageBitmap(scaledBitmap)
        }
    }

    fun loadImage(activity: Activity, imageView: ImageView, imageUrl: String): String? {


        imageView.setImageResource(0)

        val bitmap = checkImageInCache(imageUrl)
        bitmap?.let {
            loadImageIntoImageView(imageView, it)
            return null

        } ?: run {
            val tag =
                Downloader.downloadFile(imageUrl, null, null, object : DownloaderCallback<File?> {
                    override fun onResponse(file: File?) {
                        if (file == null)
                            return

                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        Downloader.memoryCache.put(imageUrl, bitmap)
                        displayImage(activity, imageView, bitmap)
                    }

                    override fun onFailure(e: IOException) {
                    }

                })
            return tag

        }
    }

    private fun displayImage(activity: Activity, imageView: ImageView, bitmap: Bitmap) {
        activity.runOnUiThread(Runnable {
            imageView.setImageBitmap(bitmap)


        })
    }
}