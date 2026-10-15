package com.nannyapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.R
import com.nannyapp.data.api.PlusApi
import com.nannyapp.data.api.dto.TranslateRequestDto
import com.nannyapp.data.preferences.AppSettings
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TranslationState {
    data object Loading : TranslationState
    data class Done(val text: String) : TranslationState
    data class Failed(val message: String) : TranslationState
}

/** Machine translation of text people wrote, into the language chosen in Settings. */
@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val api: PlusApi,
    private val settings: AppSettings,
) : ViewModel() {
    private val _results = MutableStateFlow<Map<String, TranslationState>>(emptyMap())
    val results: StateFlow<Map<String, TranslationState>> = _results.asStateFlow()

    fun translate(text: String) {
        if (_results.value[text] is TranslationState.Done || _results.value[text] == TranslationState.Loading) return
        _results.value = _results.value + (text to TranslationState.Loading)
        viewModelScope.launch {
            val state = when (val r = safeApiCall { api.translate(TranslateRequestDto(text, settings.contentLanguage)) }) {
                is Resource.Success -> TranslationState.Done(r.data.translated)
                is Resource.Error -> TranslationState.Failed(r.message)
                Resource.Loading -> TranslationState.Loading
            }
            _results.value = _results.value + (text to state)
        }
    }

    fun showOriginal(text: String) { _results.value = _results.value - text }
}

/**
 * Text with a small "Translate" link underneath. The translation replaces the text in place and
 * is labelled as machine translated, with a way back to the original.
 */
@Composable
fun TranslatableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    viewModel: TranslationViewModel = hiltViewModel(),
) {
    val results by viewModel.results.collectAsState()
    val state = results[text]
    Column(modifier) {
        Text((state as? TranslationState.Done)?.text ?: text, style = style, color = color)
        val (label, onClick) = when (state) {
            null -> stringResource(R.string.translate_action) to { viewModel.translate(text) }
            TranslationState.Loading -> stringResource(R.string.translate_working) to {}
            is TranslationState.Done -> stringResource(R.string.translate_show_original) to { viewModel.showOriginal(text) }
            is TranslationState.Failed -> state.message to { viewModel.translate(text) }
        }
        Text(
            text = if (state is TranslationState.Done) "$label · ${stringResource(R.string.translate_machine_note)}" else label,
            style = MaterialTheme.typography.labelMedium,
            color = linkColor,
            modifier = Modifier.clickable(onClick = onClick).padding(top = 4.dp, bottom = 2.dp),
        )
    }
}
