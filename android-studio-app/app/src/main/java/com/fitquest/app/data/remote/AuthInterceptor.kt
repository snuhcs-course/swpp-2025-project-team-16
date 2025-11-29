package com.fitquest.app.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.fitquest.app.MyApp
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = TokenManager.getToken(context)

        val newRequest = request.newBuilder()
            .apply {
                token?.let { header("Authorization", "Bearer $it") }
            }
            .build()

        val response = chain.proceed(newRequest)

        if (response.code == 401) {
            TokenManager.clear(context)

            Handler(Looper.getMainLooper()).post {
                MyApp.instance.goToLogin()
            }

            throw UnauthorizedException()
        }

        return response
    }
}

class UnauthorizedException : Exception("Session expiration: login required again")
