package com.nannyapp.data.api.dto

/** Every /api/ endpoint responds with this envelope shape — see backend_docs/API.md. */
data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
)

/** Body of a non-2xx response. `ref` is set on server-side failures; see docs/TROUBLESHOOTING.md. */
data class ErrorEnvelope(
    val message: String? = null,
    val ref: String? = null,
)

data class PagedData<T>(
    val items: List<T>,
    val page: Int,
    val perPage: Int,
    val total: Int,
)
