package com.fitquest.app

import android.app.Application
import android.content.Intent

class MyApp : Application() {

    companion object {
        lateinit var instance: MyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

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
