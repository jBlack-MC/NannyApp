package com.nannyapp.data.preferences

import android.content.Context
import com.nannyapp.data.db.NannyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nannyapp.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "nannyapp_session")

/**
 * Secure-ish local session store (request #35 — DataStore for session info,
 * no plaintext credentials, token/session managed here rather than SharedPreferences).
 */
@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context, private val database: NannyDatabase) {

    private val cacheBoundary = SessionCacheBoundary { storedToken() }
    private var startupSessionChecked = false

    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = intPreferencesKey("user_id")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
    }

    val authTokenFlow: Flow<String?> = flow {
        ensureStartupSession()
        emitAll(context.dataStore.data.map { it[Keys.AUTH_TOKEN] })
    }
    val userRoleFlow: Flow<UserRole?> = flow {
        ensureStartupSession()
        emitAll(context.dataStore.data.map { prefs -> prefs[Keys.USER_ROLE]?.let(UserRole::fromApi) })
    }
    val isLoggedInFlow: Flow<Boolean> = authTokenFlow.map { it != null }

    private val expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires once each time the server rejects the saved sign-in and it is cleared. */
    val sessionExpired: SharedFlow<Unit> = expired.asSharedFlow()

    /** Restore a saved login once per process. An unchecked Remember me keeps the session only
     * for the process in which the person signed in, including Activity recreation. */
    private suspend fun ensureStartupSession() = cacheBoundary.transition {
        if (!startupSessionChecked) {
            val prefs = context.dataStore.data.first()
            if (prefs[Keys.AUTH_TOKEN] != null && prefs[Keys.REMEMBER_ME] == false) {
                context.dataStore.edit { it.clear() }
                withContext(Dispatchers.IO) { database.clearAllTables() }
            }
            startupSessionChecked = true
        }
    }

    suspend fun restoreSessionRole(): UserRole? {
        ensureStartupSession()
        val prefs = context.dataStore.data.first()
        return if (prefs[Keys.AUTH_TOKEN] == null) null else prefs[Keys.USER_ROLE]?.let(UserRole::fromApi)
    }

    /**
     * Called when the server answers 401 to a request made with [token]. Only clears the session
     * if that token is still the current one, so a late reply cannot sign out a newer login.
     */
    suspend fun expireSession(token: String) {
        if (clearSessionIfCurrent(token) != null) expired.tryEmit(Unit)
    }

    suspend fun saveSession(token: String, userId: Int, role: UserRole, name: String, email: String, rememberMe: Boolean) {
        cacheBoundary.transition {
            context.dataStore.edit { it.clear() }
            withContext(Dispatchers.IO) { database.clearAllTables() }
            context.dataStore.edit { prefs ->
                prefs[Keys.AUTH_TOKEN] = token
                prefs[Keys.USER_ID] = userId
                prefs[Keys.USER_ROLE] = role.toApi()
                prefs[Keys.USER_NAME] = name
                prefs[Keys.USER_EMAIL] = email
                prefs[Keys.REMEMBER_ME] = rememberMe
            }
            startupSessionChecked = true
        }
    }

    suspend fun clearSession() = cacheBoundary.transition {
        context.dataStore.edit { it.clear() }
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    // A delayed password-change response must not clear a newer account's login.
    suspend fun clearSessionIfCurrent(token: String?) = cacheBoundary.access(token) {
        context.dataStore.edit { it.clear() }
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    suspend fun <T> withCurrentSession(token: String?, block: suspend () -> T): T? = cacheBoundary.access(token, block)

    private suspend fun storedToken(): String? = context.dataStore.data.first()[Keys.AUTH_TOKEN]
    suspend fun currentToken(): String? {
        ensureStartupSession()
        return storedToken()
    }
    suspend fun currentUserId(): Int? = context.dataStore.data.first()[Keys.USER_ID]
    suspend fun currentRole(): UserRole? = restoreSessionRole()
}
