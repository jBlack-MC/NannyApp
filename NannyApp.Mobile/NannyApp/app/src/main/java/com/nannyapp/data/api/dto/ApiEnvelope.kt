package com.nannyapp.data.api.dto

/** Every /api/ endpoint responds with this envelope shape — see backend_docs/API.md. */
data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
)

data class PagedData<T>(
    val items: List<T>,
    val page: Int,
    val perPage: Int,
    val total: Int,
)
