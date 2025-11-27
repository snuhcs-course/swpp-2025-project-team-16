package com.fitquest.app.data.remote

import com.fitquest.app.model.RankResponse
import com.fitquest.app.model.User
import retrofit2.Response
import retrofit2.http.GET

interface UserApiService{
    @GET("accounts/rankings/")
    suspend fun getRankings(): Response<List<RankResponse>>

    @GET("accounts/")
    suspend fun getProfile(): User
}
