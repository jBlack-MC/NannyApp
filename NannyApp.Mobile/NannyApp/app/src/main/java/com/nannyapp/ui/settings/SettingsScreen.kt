package com.nannyapp.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.nannyapp.BuildConfig
import com.nannyapp.R
import com.nannyapp.data.preferences.AppSettings
import com.nannyapp.data.preferences.ThemeMode
import com.nannyapp.ui.components.AppCard
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.work.scheduleNotificationSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.nannyapp.util.safeApiCall
import com.nannyapp.util.Resource
import com.nannyapp.data.api.dto.LanguageDto
import com.nannyapp.data.api.PlusApi

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: AppSettings,
    @ApplicationContext private val context: Context,
    private val plusApi: PlusApi,
) : ViewModel() {
    // Offline fallback: South Africa's languages that the translation service handles.
    private val _languages = MutableStateFlow(
        listOf("en" to "English", "zu" to "isiZulu", "xh" to "isiXhosa", "af" to "Afrikaans", "nso" to "Sepedi",
            "tn" to "Setswana", "st" to "Sesotho", "ts" to "Xitsonga", "ss" to "siSwati", "nr" to "isiNdebele")
            .map { (code, name) -> LanguageDto(code, name, name, true) },
    )
    val languages: StateFlow<List<LanguageDto>> = _languages.asStateFlow()
    private val _contentLanguage = MutableStateFlow(settings.contentLanguage)
    val contentLanguage: StateFlow<String> = _contentLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            (safeApiCall { plusApi.languages() } as? Resource.Success)?.data?.takeIf { it.isNotEmpty() }?.let { _languages.value = it }
        }
    }

    fun setContentLanguage(code: String) {
        settings.setContentLanguage(code)
        _contentLanguage.value = code
    }

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
    val backgroundSync: StateFlow<Boolean> = settings.backgroundSyncEnabled
    val languageTag: String get() = settings.languageTag

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setLanguage(tag: String) = settings.setLanguageTag(tag)
    fun setBackgroundSync(enabled: Boolean) {
        settings.setBackgroundSync(enabled)
        scheduleNotificationSync(context, enabled)
    }
}

/** Emergency lines shown in Settings and on the report screen. */
data class EmergencyLine(@StringRes val label: Int, val number: String)

val emergencyLines = listOf(
    EmergencyLine(R.string.emergency_police, "10111"),
    EmergencyLine(R.string.emergency_mobile, "112"),
    EmergencyLine(R.string.emergency_childline, "116"),
)

fun dial(context: Context, number: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onStaticPage: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val backgroundSync by viewModel.backgroundSync.collectAsState()
    var languageTag by remember { mutableStateOf(viewModel.languageTag) }

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.settings_title), onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsSection(stringResource(R.string.settings_appearance)) {
                listOf(
                    ThemeMode.SYSTEM to R.string.settings_theme_system,
                    ThemeMode.LIGHT to R.string.settings_theme_light,
                    ThemeMode.DARK to R.string.settings_theme_dark,
                ).forEach { (mode, label) ->
                    ChoiceRow(stringResource(label), selected = themeMode == mode) { viewModel.setThemeMode(mode) }
                }
            }

            SettingsSection(stringResource(R.string.settings_language), stringResource(R.string.settings_language_note)) {
                AppSettings.SUPPORTED_LANGUAGES.forEach { tag ->
                    ChoiceRow(languageName(tag), selected = languageTag == tag) {
                        if (languageTag != tag) {
                            languageTag = tag
                            viewModel.setLanguage(tag)
                            // Resources are chosen when the screen is created, so rebuild it in the new language.
                            (context as? Activity)?.recreate()
                        }
                    }
                }
            }

            val languages by viewModel.languages.collectAsState()
            val contentLanguage by viewModel.contentLanguage.collectAsState()
            var showAllLanguages by remember { mutableStateOf(false) }
            SettingsSection(stringResource(R.string.settings_translate_into), stringResource(R.string.settings_translate_note)) {
                Text(stringResource(R.string.settings_recommended_sa), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                languages.filter { it.recommended }.forEach { lang ->
                    ChoiceRow(lang.nativeName, selected = contentLanguage == lang.code) { viewModel.setContentLanguage(lang.code) }
                }
                val others = languages.filterNot { it.recommended }
                if (others.isNotEmpty()) {
                    if (showAllLanguages || others.any { it.code == contentLanguage }) {
                        Text(stringResource(R.string.settings_other_languages), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        others.forEach { lang ->
                            ChoiceRow(if (lang.nativeName == lang.name) lang.name else "${lang.nativeName} (${lang.name})", selected = contentLanguage == lang.code) {
                                viewModel.setContentLanguage(lang.code)
                            }
                        }
                    } else {
                        LinkRow(stringResource(R.string.settings_show_all_languages, others.size)) { showAllLanguages = true }
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_notifications)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_background_sync), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.settings_background_sync_detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = backgroundSync, onCheckedChange = viewModel::setBackgroundSync)
                }
            }

            SettingsSection(stringResource(R.string.settings_safety), stringResource(R.string.settings_safety_note)) {
                emergencyLines.forEach { line ->
                    LinkRow(stringResource(line.label), line.number, leadingCall = true) { dial(context, line.number) }
                }
                LinkRow(stringResource(R.string.settings_safety_page)) { onStaticPage("safety") }
            }

            SettingsSection(stringResource(R.string.settings_about)) {
                LinkRow(stringResource(R.string.settings_terms)) { onStaticPage("terms") }
                LinkRow(stringResource(R.string.settings_privacy)) { onStaticPage("privacy") }
                LinkRow(stringResource(R.string.settings_help)) { onStaticPage("faq") }
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun languageName(tag: String): String = when (tag) {
    "en" -> "English"
    "af" -> "Afrikaans"
    "zu" -> "isiZulu"
    else -> stringResource(R.string.settings_language_system)
}

@Composable
private fun SettingsSection(title: String, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 6.dp), content = content) }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinkRow(label: String, trailing: String? = null, leadingCall: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingCall) {
            Icon(Icons.Outlined.Call, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
