package com.fitquest.app.data.remote

import com.fitquest.app.model.User
import retrofit2.Response
import retrofit2.http.GET

data class RankResponse(
    val rank: Int,
    val name: String,
    val xp: Int,
    val level: Int
)

interface UserApiService{
    @GET("accounts/rankings/")
    suspend fun getRankings(): Response<List<RankResponse>>

    @GET("accounts/")
    suspend fun getProfile(): User
}
