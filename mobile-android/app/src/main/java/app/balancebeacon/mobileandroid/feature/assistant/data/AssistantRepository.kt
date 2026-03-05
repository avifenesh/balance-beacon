package app.balancebeacon.mobileandroid.feature.assistant.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatRequest
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatResponse
import kotlinx.serialization.json.Json

class AssistantRepository(
    private val assistantApi: AssistantApi
) {
    suspend fun chat(request: AssistantChatRequest): AppResult<String> {
        return runAppResult {
            val response = assistantApi.chat(request = request)
            if (!response.isSuccessful) {
                val errorMessage = response.errorBody()?.string()?.trim()
                throw IllegalStateException(
                    errorMessage?.takeIf { it.isNotBlank() }
                        ?: "Assistant request failed with status ${response.code()}"
                )
            }

            val rawBody = response.body()?.string()?.trim().orEmpty()
            parseResponseText(rawBody)
        }
    }

    private fun parseResponseText(rawBody: String): String {
        if (rawBody.isBlank()) {
            return "No response text received."
        }

        val parsed = runCatching {
            json.decodeFromString(AssistantChatResponse.serializer(), rawBody)
        }.getOrNull()

        return parsed?.message?.takeIf { it.isNotBlank() }
            ?: parsed?.text?.takeIf { it.isNotBlank() }
            ?: parsed?.response?.takeIf { it.isNotBlank() }
            ?: rawBody
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
