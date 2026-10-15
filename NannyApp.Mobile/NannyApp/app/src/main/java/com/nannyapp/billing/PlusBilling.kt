package com.nannyapp.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.nannyapp.BuildConfig
import com.nannyapp.data.api.PlusApi
import com.nannyapp.data.api.dto.PlusStatusDto
import com.nannyapp.data.api.dto.PlusVerifyRequestDto
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class PlusState(
    val loading: Boolean = true,
    /** The server's answer, the only source of truth for what is unlocked. */
    val active: Boolean = false,
    val expiresAt: String? = null,
    /** False until the server has a Google Play service account configured. */
    val serverConfigured: Boolean = true,
    /** Price as Google Play formats it, e.g. "R99.00/month"; null if Play could not supply the product. */
    val price: String? = null,
    /** Google Play is missing, signed out, or the app was not installed from Play. */
    val billingUnavailable: Boolean = false,
    val purchasing: Boolean = false,
    val message: String? = null,
)

/**
 * Nanny-App Plus through Google Play Billing. The phone only starts purchases; every purchase
 * token goes to api/subscriptions/verify.php, which checks it with Google, records it and
 * acknowledges it. Nothing is unlocked on the phone's word alone.
 */
@Singleton
class PlusBilling @Inject constructor(
    @ApplicationContext context: Context,
    private val api: PlusApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PlusState())
    val state: StateFlow<PlusState> = _state.asStateFlow()
    private var product: ProductDetails? = null
    private var accountReference: String? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
        .enableAutoServiceReconnection()
        .build()

    /** Loads the server status, the Play price, and re-sends existing purchases so renewals are recorded. */
    fun refresh() {
        scope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            when (val status = safeApiCall { api.status() }) {
                is Resource.Success -> applyStatus(status.data)
                is Resource.Error -> _state.value = _state.value.copy(message = status.message)
                Resource.Loading -> {}
            }
            if (connect()) {
                loadProduct()
                syncExistingPurchases()
            } else {
                _state.value = _state.value.copy(billingUnavailable = true)
            }
            _state.value = _state.value.copy(loading = false)
        }
    }

    fun subscribe(activity: Activity) {
        val details = product
        val offer = details?.subscriptionOfferDetails?.firstOrNull()
        if (details == null || offer == null) {
            _state.value = _state.value.copy(message = null, billingUnavailable = true)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build(),
            ))
            // Ties the purchase to this account; the server refuses tokens carrying another account's reference.
            .apply { accountReference?.let { setObfuscatedAccountId(it.take(64)) } }
            .build()
        _state.value = _state.value.copy(purchasing = true, message = null)
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(purchasing = false, message = result.debugMessage.ifBlank { null })
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { purchases.forEach { verify(it) }; _state.value = _state.value.copy(purchasing = false) }
        } else {
            _state.value = _state.value.copy(purchasing = false)
        }
    }

    private suspend fun verify(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return
        when (val result = safeApiCall { api.verify(PlusVerifyRequestDto(productId, purchase.purchaseToken)) }) {
            is Resource.Success -> applyStatus(result.data)
            is Resource.Error -> _state.value = _state.value.copy(message = result.message)
            Resource.Loading -> {}
        }
    }

    private fun applyStatus(status: PlusStatusDto) {
        accountReference = status.accountReference
        _state.value = _state.value.copy(active = status.active, expiresAt = status.expiresAt, serverConfigured = status.configured)
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    private suspend fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BuildConfig.PLAY_SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )).build()
        val details = suspendCancellableCoroutine<ProductDetails?> { cont ->
            client.queryProductDetailsAsync(params) { _, result -> if (cont.isActive) cont.resume(result.productDetailsList.firstOrNull()) }
        }
        product = details
        val phase = details?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()
        _state.value = _state.value.copy(price = phase?.formattedPrice, billingUnavailable = details == null)
    }

    private suspend fun syncExistingPurchases() {
        val purchases = suspendCancellableCoroutine<List<Purchase>> { cont ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { _, list ->
                if (cont.isActive) cont.resume(list)
            }
        }
        purchases.forEach { verify(it) }
    }
}
