package com.fitquest.app


import com.fitquest.app.data.remote.UserApiService
import com.fitquest.app.model.RankResponse
import com.fitquest.app.model.User
import retrofit2.Response

class FakeUserApiService: UserApiService {
    override suspend fun getRankings(): Response<List<RankResponse>> {
        return Response.success(listOf())
    }

    override suspend fun getProfile(): User {
        return User(0,"test","test@example.com")
    }
}