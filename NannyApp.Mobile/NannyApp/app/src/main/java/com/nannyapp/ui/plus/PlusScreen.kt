package com.nannyapp.ui.plus

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.BuildConfig
import com.nannyapp.R
import com.nannyapp.billing.PlusBilling
import com.nannyapp.billing.PlusState
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.components.AppCard
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton
import com.nannyapp.ui.components.SecondaryButton
import com.nannyapp.ui.theme.NannyGradientEnd
import com.nannyapp.ui.theme.NannyGradientStart
import com.nannyapp.util.formatDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlusViewModel @Inject constructor(
    private val billing: PlusBilling,
    sessionManager: SessionManager,
) : ViewModel() {
    val state: StateFlow<PlusState> = billing.state
    val role = MutableStateFlow(UserRole.PARENT)

    init {
        viewModelScope.launch { role.value = sessionManager.currentRole() ?: UserRole.PARENT }
        billing.refresh()
    }

    fun subscribe(activity: Activity) = billing.subscribe(activity)
    fun restore() = billing.refresh()
}

@Composable
fun PlusScreen(onBack: () -> Unit, viewModel: PlusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val role by viewModel.role.collectAsState()
    val context = LocalContext.current
    val benefits = if (role == UserRole.NANNY) {
        listOf(R.string.plus_nanny_viewers, R.string.plus_nanny_featured)
    } else {
        listOf(R.string.plus_parent_profiles, R.string.plus_parent_documents, R.string.plus_parent_saved, R.string.plus_parent_priority)
    }

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.plus_name), onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(NannyGradientStart, NannyGradientEnd)), RoundedCornerShape(18.dp)).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                Text(stringResource(R.string.plus_name), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(stringResource(R.string.plus_tagline), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .9f))
                if (state.price != null && !state.active) {
                    Text(state.price!!, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }

            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    benefits.forEach {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 2.dp).size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(it), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Text(stringResource(R.string.plus_safety_free), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when {
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.active -> {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text(stringResource(R.string.plus_active), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            state.expiresAt?.let { Text(stringResource(R.string.plus_renews, formatDateTime(it)), style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                    SecondaryButton(
                        text = stringResource(R.string.plus_manage),
                        onClick = {
                            val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=${BuildConfig.PLAY_SUBSCRIPTION_ID}&package=${context.packageName}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                !state.serverConfigured -> Notice(stringResource(R.string.plus_not_ready))
                state.billingUnavailable -> Notice(stringResource(R.string.plus_play_only))
                else -> PrimaryButton(
                    text = stringResource(R.string.plus_subscribe),
                    loading = state.purchasing,
                    onClick = { (context as? Activity)?.let(viewModel::subscribe) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            if (!state.loading && !state.active) {
                TextButton(onClick = viewModel::restore, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.plus_restore))
                }
            }
            Text(stringResource(R.string.plus_billing_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Notice(text: String) {
    AppCard(Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(18.dp))
    }
}
