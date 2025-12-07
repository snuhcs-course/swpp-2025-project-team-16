package com.fitquest.app.repository

import com.fitquest.app.data.remote.UserApiService
import com.fitquest.app.model.RankResponse
import com.fitquest.app.model.User
import retrofit2.Response

class UserRepository(private val service: UserApiService) {
    suspend fun getRankings(): Response<List<RankResponse>> =
        service.getRankings()

    suspend fun getProfile(): User =
        service.getProfile()
}