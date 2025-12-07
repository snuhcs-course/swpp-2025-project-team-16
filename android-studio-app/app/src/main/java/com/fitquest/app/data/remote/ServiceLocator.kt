package com.fitquest.app.data.remote

object ServiceLocator {
    var scheduleApiService: ScheduleApiService = RetrofitClient.scheduleApiService
    var sessionApiService: SessionApiService= RetrofitClient.sessionApiService
    var userApiService: UserApiService= RetrofitClient.userApiService
    var authApiService: AuthApiService= RetrofitClient.authApiService
    var dailySummaryApiService: DailySummaryApiService = RetrofitClient.dailySummaryApiService
    var poseAnalysisApiService: PoseAnalysisApiService = RetrofitClient.poseAnalysisApiService









}