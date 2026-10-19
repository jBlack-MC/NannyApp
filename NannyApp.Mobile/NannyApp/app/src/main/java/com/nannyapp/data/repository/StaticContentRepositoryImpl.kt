package com.nannyapp.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nannyapp.data.api.StaticContentApi
import com.nannyapp.data.api.dto.PageBlockDto
import com.nannyapp.data.api.dto.PageContentDto
import com.nannyapp.domain.model.HomeContent
import com.nannyapp.domain.model.HomeNanny
import com.nannyapp.domain.model.PageBlock
import com.nannyapp.domain.model.Testimonial
import com.nannyapp.domain.model.StaticPage
import com.nannyapp.domain.repository.StaticContentRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticContentRepositoryImpl @Inject constructor(
    private val api: StaticContentApi,
    @ApplicationContext private val context: Context,
) : StaticContentRepository {
    private val bundleLock = Mutex()
    private var bundle: Map<String, PageContentDto>? = null

    // public-pages.json is generated from the website by tests/build-public-content.py.
    override suspend fun getBundledPage(pageKey: String): StaticPage? {
        val pages = bundleLock.withLock {
            bundle ?: withContext(Dispatchers.IO) {
                context.assets.open("public-pages.json").bufferedReader().use { reader ->
                    val type = object : TypeToken<Map<String, PageContentDto>>() {}.type
                    Gson().fromJson<Map<String, PageContentDto>>(reader, type)
                }
            }.also { bundle = it }
        }
        return pages[pageKey]?.asDomain()
    }

    override suspend fun getPublishedPage(pageKey: String): Resource<StaticPage> =
        safeApiCall { api.getPage(pageKey) }.map { it.asDomain() }

    override suspend fun getHomeContent(): Resource<HomeContent> =
        safeApiCall { api.getHome() }.map { dto ->
            HomeContent(
                galleryImages = dto.galleryImages.orEmpty(),
                topNannies = dto.topNannies.orEmpty().map {
                    HomeNanny(it.userId, it.fullName, it.photoUrl, it.location, it.hourlyRate, it.averageRating, it.reviewCount, it.experienceYears)
                },
                testimonials = dto.testimonials.orEmpty().map { Testimonial(it.parentName, it.nannyName, it.comment, it.rating) },
            )
        }

    private fun PageContentDto.asDomain() =
        StaticPage(title = title ?: pageKey, body = body.orEmpty(), blocks = blocks.orEmpty().mapNotNull { it.asDomain() })

    private fun PageBlockDto.asDomain(): PageBlock? {
        val type = type ?: return null
        return PageBlock(type = type, text = text.orEmpty(), blocks = blocks.orEmpty().mapNotNull { it.asDomain() })
    }
}
