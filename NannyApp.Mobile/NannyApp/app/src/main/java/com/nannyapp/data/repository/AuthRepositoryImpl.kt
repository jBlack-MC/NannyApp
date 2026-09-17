package com.nannyapp.data.repository

import com.nannyapp.data.api.AuthApi
import com.nannyapp.data.api.dto.*
import com.nannyapp.data.db.dao.UserDao
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.model.User
import com.nannyapp.domain.model.UserRole
import com.nannyapp.domain.repository.AuthRepository
import com.nannyapp.domain.repository.RegisterData
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
) : AuthRepository {

    /** Emits the cached user for the logged-in session, or null when logged out. */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<User?>
        get() = sessionManager.authTokenFlow.distinctUntilChanged().flatMapLatest { token ->
            if (token == null) {
                flowOf(null)
            } else {
                flow {
                    val id = sessionManager.currentUserId()
                    if (id == null) emit(null) else emitAll(userDao.observe(id).map { it?.toDomain() })
                }
            }
        }

    override suspend fun login(email: String, password: String, rememberMe: Boolean): Resource<User> {
        val result = safeApiCall { api.login(LoginRequestDto(email, password, rememberMe)) }
        return when (result) {
            is Resource.Success -> {
                val dto = result.data
                val user = dto.user.toDomain()
                sessionManager.saveSession(dto.token, user.id, user.role, user.fullName, user.email, rememberMe)
                userDao.upsert(user.toEntity())
                Resource.Success(user)
            }
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun register(request: RegisterData): Resource<User> {
        val dto = RegisterRequestDto(
            role = request.role.toApi(), fullName = request.fullName, email = request.email,
            phone = request.phone, password = request.password, dateOfBirth = request.dateOfBirth,
            address = request.address, gender = request.gender.name.lowercase(),
            emergencyContact = request.emergencyContact, emergencyContactName = request.emergencyContactName,
            emergencyContactRelationship = request.emergencyContactRelationship,
            numberOfChildren = request.numberOfChildren, bio = request.bio,
            experienceYears = request.experienceYears, hourlyRate = request.hourlyRate,
            location = request.location, skills = request.skills.joinToString(","),
            languages = request.languages.joinToString(","), qualifications = request.qualifications,
            specialisations = request.specialisations.joinToString(","),
        )
        val result = safeApiCall { api.register(dto) }
        return when (result) {
            is Resource.Success -> {
                val user = result.data.user.toDomain()
                sessionManager.saveSession(result.data.token, user.id, user.role, user.fullName, user.email, true)
                userDao.upsert(user.toEntity())
                Resource.Success(user)
            }
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        sessionManager.clearSession()
    }

    override suspend fun forgotPassword(email: String): Resource<Unit> =
        safeApiCall { api.forgotPassword(ForgotPasswordRequestDto(email)) }

    override suspend fun resetPassword(token: String, newPassword: String): Resource<Unit> =
        safeApiCall { api.resetPassword(ResetPasswordRequestDto(token, newPassword)) }

    override suspend fun resendVerification(email: String): Resource<Unit> =
        safeApiCall { api.resendVerification(ResendVerificationRequestDto(email)) }

    override suspend fun verifyEmail(token: String): Resource<Unit> =
        safeApiCall { api.verifyEmail(VerifyEmailRequestDto(token)) }

    override suspend fun isLoggedIn(): Boolean = sessionManager.currentToken() != null
}
