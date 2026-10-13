package com.nannyapp.util

import com.google.gson.Gson
import com.nannyapp.data.api.dto.ApiEnvelope
import com.nannyapp.data.api.dto.ErrorEnvelope
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Wraps a Retrofit suspend call, converting network/HTTP failures into a
 * Resource<T> with friendly copy instead of letting exceptions propagate
 * and crash the app.
 */
suspend fun <T> safeApiCall(block: suspend () -> Response<ApiEnvelope<T>>): Resource<T> {
    return try {
        val response = block()
        val body = response.body()
        when {
            response.code() == 401 -> Resource.Error(ErrorMessages.UNAUTHORIZED, code = 401)
            !response.isSuccessful -> Resource.Error(response.failureMessage(), code = response.code())
            body == null -> Resource.Error(ErrorMessages.SERVER_UNAVAILABLE)
            !body.success -> Resource.Error(body.message ?: ErrorMessages.GENERIC)
            body.data == null -> Resource.Error(ErrorMessages.EMPTY)
            else -> Resource.Success(body.data)
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        Resource.Error(ErrorMessages.TIMEOUT, e)
    } catch (e: IOException) {
        Resource.Error(ErrorMessages.NO_INTERNET, e)
    } catch (e: Exception) {
        Resource.Error(ErrorMessages.GENERIC, e)
    }
}

private val errorGson = Gson()

/**
 * Retrofit leaves body() null for non-2xx responses, so the server's own
 * message lives in errorBody(). Server-side failures also carry a short
 * reference that support can match to one line in the server log.
 */
private fun Response<*>.failureMessage(): String {
    val envelope = runCatching { errorGson.fromJson(errorBody()?.string(), ErrorEnvelope::class.java) }.getOrNull()
    val message = envelope?.message?.takeIf { it.isNotBlank() } ?: ErrorMessages.SERVER_UNAVAILABLE
    val ref = envelope?.ref ?: headers()["X-Request-Ref"]
    return if (code() >= 500 && !ref.isNullOrBlank()) "$message (Reference: $ref)" else message
}
