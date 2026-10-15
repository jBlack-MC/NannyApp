package com.nannyapp.data.repository

import com.nannyapp.data.api.ReportApi
import com.nannyapp.data.api.StaticContentApi
import com.nannyapp.data.api.dto.ReportRequestDto
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Safety reports about another user, and South African public holidays for booking. */
@Singleton
class SafetyRepository @Inject constructor(
    private val reportApi: ReportApi,
    private val contentApi: StaticContentApi,
) {
    suspend fun report(reportedUserId: Int, bookingId: Int?, reason: String, details: String): Resource<Int> =
        safeApiCall { reportApi.report(ReportRequestDto(reportedUserId, bookingId, reason, details.trim())) }.map { it.id }

    private val holidayLock = Mutex()
    private val holidays = mutableMapOf<Int, Map<String, String>>()

    /** Holiday name by "yyyy-MM-dd" for [year]; empty if the list could not be loaded. */
    suspend fun holidaysFor(year: Int): Map<String, String> = holidayLock.withLock {
        holidays[year] ?: run {
            val result = safeApiCall { contentApi.getHolidays(year) }
            val byDate = (result as? Resource.Success)?.data.orEmpty().associate { it.date to it.name }
            // Only remember a successful answer, so a failed lookup is retried next time.
            if (result is Resource.Success) holidays[year] = byDate
            byDate
        }
    }
}
