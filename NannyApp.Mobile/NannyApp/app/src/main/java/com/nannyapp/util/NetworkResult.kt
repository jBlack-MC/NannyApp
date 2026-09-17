package com.nannyapp.util

import com.nannyapp.data.api.dto.ApiEnvelope
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
            !response.isSuccessful -> Resource.Error(
                body?.message ?: ErrorMessages.SERVER_UNAVAILABLE,
                code = response.code(),
            )
            body == null -> Resource.Error(ErrorMessages.SERVER_UNAVAILABLE)
            !body.success -> Resource.Error(body.message ?: ErrorMessages.GENERIC)
            body.data == null -> Resource.Error(ErrorMessages.EMPTY)
            else -> Resource.Success(body.data)
        }
    } catch (e: SocketTimeoutException) {
        Resource.Error(ErrorMessages.TIMEOUT, e)
    } catch (e: IOException) {
        Resource.Error(ErrorMessages.NO_INTERNET, e)
    } catch (e: Exception) {
        Resource.Error(ErrorMessages.GENERIC, e)
    }
}
