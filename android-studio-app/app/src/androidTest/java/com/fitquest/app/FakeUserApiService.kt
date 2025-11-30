package com.fitquest.app

import com.fitquest.app.data.remote.RankResponse
import com.fitquest.app.data.remote.UserApiService
import com.fitquest.app.model.User
import retrofit2.Response

class FakeUserApiService: UserApiService {
    override suspend fun getRankings(): Response<List<RankResponse>> {
        TODO("Not yet implemented")
    }

    override suspend fun getProfile(): User {
        TODO("Not yet implemented")
    }
}