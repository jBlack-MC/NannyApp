package com.nannyapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.nannyapp.data.preferences.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/** Exposes session expiry to the navigation graph, which has no other access to SessionManager. */
@HiltViewModel
class SessionEventsViewModel @Inject constructor(sessionManager: SessionManager) : ViewModel() {
    val sessionExpired: SharedFlow<Unit> = sessionManager.sessionExpired
}
