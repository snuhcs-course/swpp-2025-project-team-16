package com.fitquest.app

import android.app.Application
import android.content.Intent
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApp : Application() {

    companion object {
        lateinit var instance: MyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        AndroidThreeTen.init(this)

        CurrentActivityHelper.init(this)
    }

    fun goToLogin() {
        val currentActivity = CurrentActivityHelper.currentActivity ?: return

        val intent = Intent(currentActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        currentActivity.startActivity(intent)
    }
}
