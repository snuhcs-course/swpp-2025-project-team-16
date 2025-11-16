package com.fitquest.app.data.remote

import com.fitquest.app.MyApp
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://147.46.78.29:8004/" //"http://10.0.2.2:8001/"

    // ✅ OkHttpClient에 timeout 설정 추가
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            TokenManager.getToken(MyApp.instance)?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }
        .connectTimeout(30, TimeUnit.SECONDS)// 연결 시도 최대 30초
        .readTimeout(90, TimeUnit.SECONDS)    // 서버 응답 대기 최대 30초
        .writeTimeout(30, TimeUnit.SECONDS)   // 요청 전송 최대 30초
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val journeyApiService: JourneyApiService by lazy {
        retrofit.create(JourneyApiService::class.java)
    }

    val profileApiService: ProfileApiService by lazy {
        retrofit.create(ProfileApiService::class.java)
    }
    val scheduleApiService: ScheduleApiService by lazy {
        retrofit.create(ScheduleApiService::class.java)
    }

    val sessionApiService: SessionApiService by lazy {
        retrofit.create(SessionApiService::class.java)
    }

}
