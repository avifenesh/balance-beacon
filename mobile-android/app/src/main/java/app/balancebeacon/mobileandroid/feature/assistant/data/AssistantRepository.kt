package app.balancebeacon.mobileandroid.feature.assistant.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatRequest
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json

class AssistantRepository(
    private val assistantApi: AssistantApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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

    suspend fun streamChat(
        request: AssistantChatRequest,
        onPartial: (String) -> Unit
    ): AppResult<String> {
        return runAppResult {
            withContext(ioDispatcher) {
                val response = assistantApi.chat(request = request)
                if (!response.isSuccessful) {
                    val errorMessage = response.errorBody()?.string()?.trim()
                    throw IllegalStateException(
                        errorMessage?.takeIf { it.isNotBlank() }
                            ?: "Assistant request failed with status ${response.code()}"
                    )
                }

                val body = response.body() ?: return@withContext "No response text received."
                parseStreamText(body = body, onPartial = onPartial)
            }
        }
    }

    private suspend fun parseStreamText(
        body: okhttp3.ResponseBody,
        onPartial: (String) -> Unit
    ): String {
        val assistantContent = StringBuilder()
        val fallbackContent = StringBuilder()

        body.use { responseBody ->
            responseBody.charStream().buffered().useLines { lines ->
                lines.forEach { line ->
                    currentCoroutineContext().ensureActive()
                    val trimmed = line.trim()
                    if (trimmed.isBlank()) {
                        return@forEach
                    }

                    if (trimmed.startsWith("0:")) {
                        val delta = parseTextDelta(trimmed.removePrefix("0:"))
                        if (!delta.isNullOrBlank()) {
                            assistantContent.append(delta)
                            onPartial(assistantContent.toString())
                        }
                    } else {
                        if (fallbackContent.isNotEmpty()) {
                            fallbackContent.append('\n')
                        }
                        fallbackContent.append(trimmed)
                    }
                }
            }
        }

        if (assistantContent.isNotEmpty()) {
            return assistantContent.toString()
        }

        val fallback = fallbackContent.toString().trim()
        return if (fallback.isBlank()) {
            "No response text received."
        } else {
            parseResponseText(fallback)
        }
    }

    private fun parseTextDelta(payload: String): String? {
        val element = runCatching { json.parseToJsonElement(payload) }.getOrNull() ?: return null

        return when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonObject -> {
                val type = element["type"]?.jsonPrimitive?.contentOrNull
                when {
                    type == "text-delta" -> element["textDelta"]?.jsonPrimitive?.contentOrNull
                    else -> element["text"]?.jsonPrimitive?.contentOrNull
                        ?: element["content"]?.jsonPrimitive?.contentOrNull
                        ?: element["message"]?.jsonPrimitive?.contentOrNull
                        ?: element["response"]?.jsonPrimitive?.contentOrNull
                }
            }

            else -> null
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
