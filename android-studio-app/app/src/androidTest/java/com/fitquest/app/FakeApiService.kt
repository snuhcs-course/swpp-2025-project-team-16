package com.fitquest.app

import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.EmailCheckResponse
import com.fitquest.app.data.remote.EvaluatePostureRequest
import com.fitquest.app.data.remote.EvaluatePostureResponse
import com.fitquest.app.data.remote.InitialCountRequest
import com.fitquest.app.data.remote.InitialCountResponse
import com.fitquest.app.data.remote.LoginRequest
import com.fitquest.app.data.remote.LoginResponse
import com.fitquest.app.data.remote.SignupRequest
import com.fitquest.app.data.remote.SignupResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeApiService: ApiService{
    override suspend fun signup(request: SignupRequest): Response<SignupResponse> {
        return if(request.name=="test") Response.success(SignupResponse("well","Not Null"))
        else if(request.name=="error") Response.error(401,"unauthorized".toResponseBody())
        else Response.success(SignupResponse("well",null))
    }

    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return if(request.password=="0001"){ Response.error(401,"unauthorized".toResponseBody())}
            else if(request.password=="error"){Response.success(LoginResponse(null,null,null,null,null))}
            else Response.success(LoginResponse("Not Null",null,null,null,null))
    }

    override suspend fun checkEmail(request: Map<String, String>): Response<EmailCheckResponse> {
        return if(request.values.contains("snu@snu.com")){Response.success(EmailCheckResponse(true))}
        else if(request.values.contains("error@error.com")){Response.error(401,"unauthorized".toResponseBody()) }
        else{Response.success(EmailCheckResponse(false))}
    }

    override suspend fun updateInitialReps(
        token: String,
        body: InitialCountRequest
    ): Response<InitialCountResponse> {
        return Response.success(InitialCountResponse("well",0))
    }

    override suspend fun evaluatePosture(
        fullUrl: String,
        request: EvaluatePostureRequest
    ): Response<EvaluatePostureResponse> {
        return Response.success(EvaluatePostureResponse(status = "well", good_points = "none", improvement_points = "none", improvement_methods = "none"))
    }

}