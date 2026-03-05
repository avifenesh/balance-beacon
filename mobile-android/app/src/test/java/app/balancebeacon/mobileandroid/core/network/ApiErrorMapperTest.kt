package app.balancebeacon.mobileandroid.core.network

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorMapperTest {
    @Test
    fun toException_parsesErrorPayloadFields() {
        val payload = """
            {
              "message": "Validation failed",
              "fields": {
                "email": "Invalid email"
              }
            }
        """.trimIndent()

        val exception = ApiErrorMapper.toException(httpCode = 422, body = payload)

        assertEquals(422, exception.httpCode)
        assertEquals("Validation failed", exception.message)
        assertEquals("Invalid email", exception.fieldErrors["email"])
    }

    @Test
    fun toAppError_mapsIoExceptionToNetworkMessage() {
        val error = ApiErrorMapper.toAppError(IOException("timeout"))

        assertEquals("Network request failed", error.message)
        assertTrue(error.cause is IOException)
    }
}

