package com.fitquest.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

data class RankResponse(
    val rank: Int,
    val name: String,
    val xp: Int,
    val level: Int
)

data class HistoryResponse(
    val id: Int,
    val date: String,
    val name: String,
    val is_finished: Boolean,
    val start_time: String?,
    val end_time: String?,
    val xp: Int? = null,  // 나중에 추가할 수도
    val percent: String? = null // 임시 placeholder
)

interface ProfileApiService{
    @GET("accounts/rankings/")
    suspend fun getRankings(
        @Header("Authorization") token: String
    ): Response<List<RankResponse>>

    @GET("schedule/history/")
    suspend fun getUserHistory(
        @Header("Authorization") token: String
    ): Response<List<HistoryResponse>>

}
