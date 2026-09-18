package com.nannyapp.data.repository

import com.nannyapp.data.api.UserApi
import com.nannyapp.data.db.dao.UserDao
import com.nannyapp.domain.model.User
import com.nannyapp.domain.repository.UserRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val userDao: UserDao,
) : UserRepository {

    override suspend fun getProfile(): Resource<User> {
        val result = safeApiCall { api.getProfile() }
        if (result is Resource.Success) userDao.upsert(result.data.asDomain().toEntity())
        return result.map { it.asDomain() }
    }

    override suspend fun updateProfile(user: User): Resource<User> {
        val result = safeApiCall { api.updateProfile(user.toDto()) }
        if (result is Resource.Success) userDao.upsert(result.data.asDomain().toEntity())
        return result.map { it.asDomain() }
    }

    override suspend fun changePassword(current: String, new: String): Resource<Unit> =
        safeApiCall { api.changePassword(mapOf("current_password" to current, "new_password" to new)) }

    override suspend fun uploadProfileImage(bytes: ByteArray, fileName: String): Resource<String> {
        val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("avatar", fileName, body)
        val result = safeApiCall { api.uploadAvatar(part) }
        return result.map { it["url"] ?: "" }
    }

    /** Mirrors the completion checklist implied by account.php: name, phone, DOB, address,
     * gender, and profile photo each contribute equally. */
    override fun profileCompletionPercent(user: User, extra: Map<String, Boolean>): Int {
        val fields = listOf(
            user.fullName.isNotBlank(),
            user.phone?.isNotBlank() == true,
            user.dateOfBirth?.isNotBlank() == true,
            user.address?.isNotBlank() == true,
            user.profileImageUrl?.isNotBlank() == true,
            user.emailVerified,
        ) + extra.values
        val filled = fields.count { it }
        return if (fields.isEmpty()) 0 else (filled * 100) / fields.size
    }
}

