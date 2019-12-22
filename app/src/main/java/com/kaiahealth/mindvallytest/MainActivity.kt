package com.kaiahealth.mindvallytest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.kaiahealth.mindvallytest.models.Response
import com.shenawynkov.downloadany.DownloaderCallback
import com.shenawynkov.downloadany.Downloader
import com.shenawynkov.downloadany.ImageLoader
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Downloader.getObject("https://pastebin.com/raw/wgkJgazE",Response::class.java,object :DownloaderCallback<ArrayList<Response>>{
            override fun onResponse(response:ArrayList<Response>) {
                Log.d("showResponse",response.get(0).user.profile_image.large)


              val tag= ImageLoader.loadImage(this@MainActivity,imageView,response.get(0).user.profile_image.large)
                 //  call?.cancel()
           //     Downloader.cancelCallWithTag(tag)

            }

            override fun onFailure(e: IOException) {
                Log.d("showResponse",e.toString())
            }

        })
    }
}
