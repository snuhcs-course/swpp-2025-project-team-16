package com.fitquest.app


import com.fitquest.app.data.remote.UserApiService
import com.fitquest.app.model.RankResponse
import com.fitquest.app.model.User
import retrofit2.Response

class FakeUserApiService: UserApiService {
    override suspend fun getRankings(): Response<List<RankResponse>> {
        return Response.success(listOf(RankResponse(1,"test",100,1)))
    }

    override suspend fun getProfile(): User {
        return User(0,"test","test@example.com")
    }
}