package com.shenawynkov.downloadany

import java.io.IOException

interface DownloaderCallback<in T> {
    fun onResponse(response: T)
    fun onFailure(e: IOException)
}