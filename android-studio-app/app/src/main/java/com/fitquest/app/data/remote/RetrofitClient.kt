package com.fitquest.app.data.remote

import com.fitquest.app.MyApp
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.OkHttpClient
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://147.46.78.29:8004/" // "http://10.0.2.2:8001/"

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

    val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
            LocalDate.parse(json.asString)
        })
        .registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json, _, _ ->
            LocalTime.parse(json.asString)
        })
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
            LocalDateTime.parse(json.asString)
        })
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
            val str = json.asString
            try {
                OffsetDateTime.parse(str).toLocalDateTime()
            } catch (e: Exception) {
                LocalDateTime.parse(str) // fallback
            }
        })
        .create()


    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val userApiService: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }
    val scheduleApiService: ScheduleApiService by lazy {
        retrofit.create(ScheduleApiService::class.java)
    }

    val sessionApiService: SessionApiService by lazy {
        retrofit.create(SessionApiService::class.java)
    }

    val poseAnalysisApiService: PoseAnalysisApiService by lazy {
        retrofit.create(PoseAnalysisApiService::class.java)
    }

    val dailySummaryApiService: DailySummaryApiService by lazy {
        retrofit.create(DailySummaryApiService::class.java)
    }
}
