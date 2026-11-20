package com.ktp.iptv

import android.app.Application
import com.ktp.iptv.data.AppContainer
import com.ktp.iptv.data.DefaultAppContainer

class KtpIptvApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
