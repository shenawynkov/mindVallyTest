package com.shenawynkov.downloadany

import android.graphics.Bitmap
import android.webkit.MimeTypeMap
import androidx.collection.LruCache
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.*
import okio.buffer
import okio.sink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


public object Downloader {
    private val maxCacheSize: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
    val memoryCache: LruCache<String, Any>
    val client:OkHttpClient

    init {
        client = OkHttpClient()

        memoryCache = object : LruCache<String, Any>(maxCacheSize) {
            override fun sizeOf(key: String, bitmap: Any): Int {
                // The cache size will be measured in kilobytes rather than number of items.
                if (bitmap is Bitmap)
                    return bitmap.byteCount / 1024
                else if (bitmap is String)
                    return bitmap.length / 1024
                return 0
            }
        }
    }

    private fun GET(url: String, callback: Callback): String {
        val request = Request.Builder()
            .url(url)
            .tag(System.currentTimeMillis().toString())
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)

        return call.request().tag() as String
    }


    fun <T : Any, P : Any> getObject(
        url: String,
        clazz: Class<P>,
        callback: DownloaderCallback<T>
    ) :String?{

        if (checkJsonInCash(url)) {
            val result: String? = memoryCache.get(url) as String
            result?.let { callback.onResponse(parseObject<T, P>(it, clazz)) }

            return null

        }

       return GET(url, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val result: String? = response.body?.string()

                memoryCache.put(url, response)
                result?.let { callback.onResponse(parseObject<T, P>(it, clazz)) }


            }
        })
    }

    private fun <T : Any, P : Any> parseObject(result: String, clazz: Class<P>): T {
        if (result?.get(0) == '[') {
            val arrayList = ArrayList<P>()
            val jsonArray = JSONArray(result)
            val gson = Gson()
            for (i in 0 until jsonArray.length()) {
                val explrObject: JSONObject = jsonArray.getJSONObject(i)
                val `object`: P = gson.fromJson(explrObject.toString(), clazz) as P

                arrayList.add(`object`)
            }
            return arrayList as T
        } else {
            val gson = GsonBuilder()


            val myJson: T = gson.create().fromJson(result, clazz) as T
            return myJson
        }

    }

     fun downloadFile(
        url: String, name: String?, fileExt: String?,
        callback: DownloaderCallback<File?>
    ): String? {
        val tag =
            GET(url, object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onFailure(e)

                }

                override fun onResponse(call: Call, response: Response) {
                    val contentType = response.header("content-type", null)
                    var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                    ext = if (ext == null) {
                        fileExt
                    } else {
                        ".$ext"
                    }

                    // use provided name or generate a temp file
                    var file: File? = null
                    file = if (name != null) {
                        val filename = String.format("%s%s", name, ext)
                        File(filename)
                    } else {
                        val s = SimpleDateFormat("ddMMyyyyhhmmss")
                        val timestamp = s.format(Date())
                        File.createTempFile(timestamp, ext)
                    }

                    val body = response.body
                    val sink = file?.let { it.sink().buffer() }
                    /*
                    sink.writeAll(body!!.source())
                    sink.close()
                    body.close()
                     */

                    body?.source().use { input ->
                        sink.use { output ->
                            if (input != null) {
                                output?.writeAll(input)
                            }
                        }
                    }
                    callback.onResponse(file)
                }

            })
        return tag;
    }


    private  fun checkJsonInCash(url: String):Boolean
    {
        return memoryCache.get(url) != null
    }

    fun cancelCallWithTag( tag: String?) {
        for (call in client.dispatcher.queuedCalls()) {
            if (call.request().tag()!!.equals(tag)) call.cancel()
        }
        for (call in client.dispatcher.runningCalls()) {
            if (call.request().tag()!!.equals(tag)) call.cancel()
        }
    }
}