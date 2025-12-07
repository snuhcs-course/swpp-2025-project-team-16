package com.fitquest.app

import com.fitquest.app.data.remote.AuthApiService
import com.fitquest.app.model.login.EmailCheckResponse
import com.fitquest.app.model.login.InitialCountRequest
import com.fitquest.app.model.login.InitialCountResponse
import com.fitquest.app.model.login.LoginRequest
import com.fitquest.app.model.login.LoginResponse
import com.fitquest.app.model.login.SignupRequest
import com.fitquest.app.model.login.SignupResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeApiService: AuthApiService {
    override suspend fun signup(request: SignupRequest): Response<SignupResponse> {
        return when (request.name) {
            "test" -> Response.success(SignupResponse("well","Not Null"))
            "error" -> Response.error(401,"unauthorized".toResponseBody())
            else -> Response.success(SignupResponse("well",null))
        }
    }

    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return when (request.password) {
            "0001" -> Response.error(401,"unauthorized".toResponseBody())
            "error" -> Response.success(LoginResponse(null,null,null,null,null))
            else -> Response.success(LoginResponse("Not Null",null,null,null,null))
        }
    }

    override suspend fun checkEmail(request: Map<String, String>): Response<EmailCheckResponse> {
        return if(request.values.contains("snu@snu.com")){Response.success(EmailCheckResponse(true))}
        else if(request.values.contains("error@error.com")){Response.error(401,"unauthorized".toResponseBody()) }
        else{Response.success(EmailCheckResponse(false))}
    }

    override suspend fun updateInitialReps(
        body: InitialCountRequest
    ): Response<InitialCountResponse> {
        return Response.success(InitialCountResponse("well",0))
    }
}