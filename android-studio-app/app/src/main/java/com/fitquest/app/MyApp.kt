package com.fitquest.app

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApp : Application() {

    companion object {
        lateinit var instance: MyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // ThreeTenABP 초기화
        AndroidThreeTen.init(this)
    }
}
