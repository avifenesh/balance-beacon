package app.balancebeacon.mobileandroid.feature.assistant.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatRequest
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantMessageDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantRepositoryTest {
    @Test
    fun streamChat_emitsIncrementalTextDeltaAndReturnsFinalText() = runTest {
        val repository = AssistantRepository(
            assistantApi = FakeAssistantApi(
                body = """
                    0:{"type":"text-delta","textDelta":"Hello"}
                    0:{"type":"text-delta","textDelta":" there"}
                """.trimIndent()
            )
        )
        val partials = mutableListOf<String>()

        val result = repository.streamChat(sampleRequest()) { partials += it }

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("Hello", "Hello there"), partials)
        assertEquals("Hello there", (result as AppResult.Success).value)
    }

    @Test
    fun streamChat_fallsBackToJsonPayloadWhenNoStreamDeltasExist() = runTest {
        val repository = AssistantRepository(
            assistantApi = FakeAssistantApi(body = """{"message":"Fallback reply"}""")
        )

        val result = repository.streamChat(sampleRequest()) { }

        assertTrue(result is AppResult.Success)
        assertEquals("Fallback reply", (result as AppResult.Success).value)
    }

    private fun sampleRequest(): AssistantChatRequest {
        return AssistantChatRequest(
            messages = listOf(AssistantMessageDto(role = "user", content = "Hi")),
            accountId = "acc_1",
            monthKey = "2026-03"
        )
    }

    private class FakeAssistantApi(
        private val body: String
    ) : AssistantApi {
        override suspend fun chat(request: AssistantChatRequest): Response<okhttp3.ResponseBody> {
            return Response.success(body.toResponseBody("text/plain".toMediaType()))
        }
    }
}
