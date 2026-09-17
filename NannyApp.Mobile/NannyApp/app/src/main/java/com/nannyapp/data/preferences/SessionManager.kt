package com.nannyapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nannyapp.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "nannyapp_session")

/**
 * Secure-ish local session store (request #35 — DataStore for session info,
 * no plaintext credentials, token/session managed here rather than SharedPreferences).
 */
@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = intPreferencesKey("user_id")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
    }

    val authTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] }
    val userRoleFlow: Flow<UserRole?> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_ROLE]?.let { UserRole.fromApi(it) }
    }
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] != null }

    suspend fun saveSession(token: String, userId: Int, role: UserRole, name: String, email: String, rememberMe: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_ROLE] = role.toApi()
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.REMEMBER_ME] = rememberMe
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun currentToken(): String? = context.dataStore.data.first()[Keys.AUTH_TOKEN]
    suspend fun currentUserId(): Int? = context.dataStore.data.first()[Keys.USER_ID]
    suspend fun currentRole(): UserRole? = context.dataStore.data.first()[Keys.USER_ROLE]?.let { UserRole.fromApi(it) }
}
