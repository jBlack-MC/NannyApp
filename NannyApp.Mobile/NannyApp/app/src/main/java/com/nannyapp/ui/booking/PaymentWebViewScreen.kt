package com.nannyapp.ui.booking

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.repository.PaymentRepository
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.LoadingView
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the Paystack checkout returned by PaymentRepository.initializePayment().
 * The secret key never touches the app (request #23); this screen only loads
 * the public authorization_url and, once Paystack redirects back to our
 * callback URL, calls verify.php to confirm the transaction server-side.
 */
@HiltViewModel
class PaymentVerifyViewModel @Inject constructor(private val repository: PaymentRepository) : ViewModel() {
    private val _verified = MutableStateFlow<Resource<Unit>?>(null)
    val verified: StateFlow<Resource<Unit>?> = _verified.asStateFlow()

    fun verify(reference: String) {
        viewModelScope.launch {
            _verified.value = Resource.Loading
            _verified.value = repository.verifyPayment(reference).map { }
        }
    }
}

@Composable
fun PaymentWebViewScreen(
    checkoutUrl: String,
    callbackUrlPrefix: String = "nannyapp://payment-callback",
    onBack: () -> Unit,
    onVerified: () -> Unit,
    viewModel: PaymentVerifyViewModel = hiltViewModel(),
) {
    val verified by viewModel.verified.collectAsState()
    LaunchedEffect(verified) {
        if (verified is Resource.Success) onVerified()
    }

    Scaffold(topBar = { AppTopBar(title = "Secure payment", onBack = onBack) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            if (url != null && url.startsWith(callbackUrlPrefix)) {
                                val reference = Uri_getQueryParam(url, "reference")
                                if (reference != null) viewModel.verify(reference)
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(checkoutUrl)
                }
            })
            if (verified is Resource.Loading) LoadingView(label = "Confirming payment…")
        }
    }
}

private fun Uri_getQueryParam(url: String, key: String): String? =
    runCatching { android.net.Uri.parse(url).getQueryParameter(key) }.getOrNull()
