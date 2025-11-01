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

interface ProfileApiService{
    @GET("api/accounts/rankings/")
    suspend fun getRankings(
        @Header("Authorization") token: String
    ): Response<List<RankResponse>>
}
