package com.fitquest.app.data.remote

object ServiceLocator {
    var apiService: ScheduleApiService = RetrofitClient.scheduleApiService
    var sessionApiService: SessionApiService= RetrofitClient.sessionApiService
    var userApiService: UserApiService= RetrofitClient.userApiService
    var authApiService: ApiService= RetrofitClient.apiService



}