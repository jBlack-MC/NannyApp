package com.nannyapp.data.repository

import com.nannyapp.data.api.SupportApi
import com.nannyapp.data.api.dto.SubmitTicketRequestDto
import com.nannyapp.domain.model.SupportCategory
import com.nannyapp.domain.model.SupportTicket
import com.nannyapp.domain.repository.SupportRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepositoryImpl @Inject constructor(private val api: SupportApi) : SupportRepository {

    override suspend fun submitTicket(category: SupportCategory, subject: String, message: String, name: String, email: String): Resource<SupportTicket> =
        safeApiCall { api.submitTicket(SubmitTicketRequestDto(category.toApi(), subject, message, name, email)) }.map { it.toDomain() }

    override fun getMyTickets(): Flow<Resource<List<SupportTicket>>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall { api.getMyTickets() }.map { list -> list.map { it.toDomain() } })
    }
}
