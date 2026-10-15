package com.nannyapp.ui.safety

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.R
import com.nannyapp.data.repository.SafetyRepository
import com.nannyapp.ui.components.AppCard
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton
import com.nannyapp.ui.settings.dial
import com.nannyapp.ui.settings.emergencyLines
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Reason codes accepted by api/reports/create.php (NannyApp.Shared/config/reports.php). */
private val reasons = listOf(
    "child_safety" to R.string.report_reason_child_safety,
    "unsafe_behaviour" to R.string.report_reason_unsafe,
    "harassment" to R.string.report_reason_harassment,
    "fake_profile" to R.string.report_reason_fake,
    "payment" to R.string.report_reason_payment,
    "no_show" to R.string.report_reason_no_show,
    "other" to R.string.report_reason_other,
)

data class ReportUiState(
    val reason: String? = null,
    val details: String = "",
    val sending: Boolean = false,
    val error: String? = null,
    val sent: Boolean = false,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SafetyRepository,
) : ViewModel() {
    val reportedUserId: Int = checkNotNull(savedStateHandle["userId"])
    val reportedName: String = savedStateHandle.get<String>("name").orEmpty()
    private val bookingId: Int? = savedStateHandle.get<Int>("bookingId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    fun setReason(code: String) { _state.value = _state.value.copy(reason = code, error = null) }
    fun setDetails(text: String) { _state.value = _state.value.copy(details = text.take(2000), error = null) }

    fun send(@StringRes missingReason: Int, @StringRes tooShort: Int, resolve: (Int) -> String) {
        val s = _state.value
        when {
            s.reason == null -> { _state.value = s.copy(error = resolve(missingReason)); return }
            s.details.trim().length < 10 -> { _state.value = s.copy(error = resolve(tooShort)); return }
        }
        viewModelScope.launch {
            _state.value = s.copy(sending = true, error = null)
            _state.value = when (val result = repository.report(reportedUserId, bookingId, s.reason!!, s.details)) {
                is Resource.Success -> _state.value.copy(sending = false, sent = true)
                is Resource.Error -> _state.value.copy(sending = false, error = result.message)
                Resource.Loading -> _state.value
            }
        }
    }
}

@Composable
fun ReportScreen(onBack: () -> Unit, viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val name = viewModel.reportedName.ifBlank { stringResource(R.string.report_this_person) }

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.report_title), onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Emergencies first: a report is reviewed by people, not answered instantly.
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.report_danger_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.report_danger_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    emergencyLines.forEach { line ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { dial(context, line.number) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Call, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(line.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(line.number, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (state.sent) {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text(stringResource(R.string.report_sent_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.report_sent_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                PrimaryButton(text = stringResource(R.string.action_done), onClick = onBack, modifier = Modifier.fillMaxWidth())
                return@Column
            }

            Text(stringResource(R.string.report_heading, name), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.report_confidential), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(stringResource(R.string.report_reason_label), style = MaterialTheme.typography.titleMedium)
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    reasons.forEach { (code, label) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                .selectable(selected = state.reason == code, onClick = { viewModel.setReason(code) }, role = Role.RadioButton)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.reason == code, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.details,
                onValueChange = viewModel::setDetails,
                label = { Text(stringResource(R.string.report_details_label)) },
                placeholder = { Text(stringResource(R.string.report_details_hint)) },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(
                text = stringResource(R.string.report_send),
                loading = state.sending,
                onClick = { viewModel.send(R.string.report_error_reason, R.string.report_error_details) { context.getString(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
