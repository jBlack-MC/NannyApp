package com.nannyapp.data.repository

import com.nannyapp.data.api.StaticContentApi
import com.nannyapp.domain.repository.StaticContentRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticContentRepositoryImpl @Inject constructor(private val api: StaticContentApi) : StaticContentRepository {
    override suspend fun getPage(pageKey: String): Resource<Pair<String, String>> =
        safeApiCall { api.getPage(pageKey) }.map { (it.title ?: pageKey) to (it.body ?: "") }
}
