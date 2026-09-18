package com.nannyapp.data.repository

import com.nannyapp.data.api.PaymentApi
import com.nannyapp.data.db.dao.PaymentDao
import com.nannyapp.domain.model.Payment
import com.nannyapp.domain.repository.PaymentInit
import com.nannyapp.domain.repository.PaymentRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Payments are always initialized/verified server-side against Paystack â€”
 * the Android app never sees a secret key (request #23/#35). The backend
 * returns an authorization_url which we open in a Custom Tab / WebView
 * purely to complete the hosted checkout, then we call verify.php.
 */
@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val api: PaymentApi,
    private val dao: PaymentDao,
) : PaymentRepository {

    override suspend fun initializePayment(bookingId: Int): Resource<PaymentInit> =
        safeApiCall { api.initializePayment(mapOf("booking_id" to bookingId)) }
            .map { PaymentInit(it.authorizationUrl, it.reference, it.accessCode) }

    override suspend fun verifyPayment(reference: String): Resource<Payment> =
        safeApiCall { api.verifyPayment(reference) }.map { it.asDomain() }

    override fun getPaymentsForRole(): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        when (val result = safeApiCall { api.getPayments() }) {
            is Resource.Success -> {
                dao.upsertAll(result.data.map { it.toEntity() })
                emit(Resource.Success(result.data.map { it.asDomain() }))
            }
            is Resource.Error -> {
                val cached = dao.observeAll().first()
                if (cached.isNotEmpty()) emit(Resource.Success(cached.map { it.asDomain() })) else emit(result)
            }
            Resource.Loading -> {}
        }
    }
}

