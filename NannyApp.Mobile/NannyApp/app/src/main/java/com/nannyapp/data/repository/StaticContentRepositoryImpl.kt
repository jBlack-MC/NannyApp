package com.nannyapp.data.repository

import com.nannyapp.data.api.StaticContentApi
import com.nannyapp.domain.repository.StaticContentRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nannyapp.data.api.dto.PageContentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class StaticContentRepositoryImpl @Inject constructor(
    private val api: StaticContentApi,
    @ApplicationContext private val context: Context,
) : StaticContentRepository {
    override suspend fun getPage(pageKey: String): Resource<Pair<String, String>> {
        val result = withTimeoutOrNull(5_000) { safeApiCall { api.getPage(pageKey) } }
        if (result is Resource.Success && !result.data.body.isNullOrBlank()
            && result.data.body != "This page has not been published yet. Please check back soon.") {
            return result.map { (it.title ?: pageKey) to (it.body ?: "") }
        }
        val bundled = withContext(Dispatchers.IO) {
            context.assets.open("public-pages.json").bufferedReader().use { reader ->
                val type = object : TypeToken<Map<String, PageContentDto>>() {}.type
                Gson().fromJson<Map<String, PageContentDto>>(reader, type)[pageKey]
            }
        }
        return bundled?.let { Resource.Success((it.title ?: pageKey) to (it.body ?: "")) }
            ?: result?.map { (it.title ?: pageKey) to (it.body ?: "") }
            ?: Resource.Error(com.nannyapp.util.ErrorMessages.TIMEOUT)
    }
}
