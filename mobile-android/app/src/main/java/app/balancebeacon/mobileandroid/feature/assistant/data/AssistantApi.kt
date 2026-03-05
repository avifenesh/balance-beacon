package app.balancebeacon.mobileandroid.feature.assistant.data

import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AssistantApi {
    @POST("/api/chat")
    suspend fun chat(
        @Body request: AssistantChatRequest
    ): Response<ResponseBody>
}
