package com.nannyapp.ui.auth

import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.AuthRepository
import com.nannyapp.domain.repository.RegisterData
import com.nannyapp.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LoginViewModel and RegisterViewModel.
 * Uses a fake AuthRepository and TestCoroutineDispatcher to avoid real coroutines / Android deps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Shared test user ─────────────────────────────────────────────────────

    private val testUser = User(
        id = 1, fullName = "Test User", email = "test@example.com",
        phone = null, role = UserRole.PARENT, status = AccountStatus.ACTIVE,
        emailVerified = true, profileImageUrl = null, dateOfBirth = null,
        address = null, gender = Gender.UNSPECIFIED, createdAt = null,
    )

    // ── Fake repository ──────────────────────────────────────────────────────

    private fun fakeRepo(
        loginResult: Resource<User> = Resource.Success(testUser),
        registerResult: Resource<User> = Resource.Success(testUser),
        googleResult: Resource<User> = Resource.Success(testUser),
    ): AuthRepository = object : AuthRepository {
        override val currentUser: Flow<User?> = flowOf(null)
        override suspend fun login(email: String, password: String, rememberMe: Boolean) = loginResult
        override suspend fun googleLogin(idToken: String) = googleResult
        override suspend fun register(request: RegisterData) = registerResult
        override suspend fun logout() {}
        override suspend fun forgotPassword(email: String) = Resource.Success(Unit)
        override suspend fun resetPassword(token: String, newPassword: String) = Resource.Success(Unit)
        override suspend fun resendVerification(email: String) = Resource.Success(Unit)
        override suspend fun verifyEmail(token: String) = Resource.Success(Unit)
        override suspend fun isLoggedIn() = false
    }

    // ── LoginViewModel — blank input validation ──────────────────────────────

    @Test
    fun `login with blank email shows validation error without calling API`() {
        val vm = LoginViewModel(fakeRepo())
        vm.onEmailChange("")
        vm.onPasswordChange("password123")
        vm.login()
        val state = vm.state.value
        assertNotNull("Expected a validation error", state.error)
        assertNull("Should not be logged in", state.loggedInRole)
    }

    @Test
    fun `login with blank password shows validation error`() {
        val vm = LoginViewModel(fakeRepo())
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("")
        vm.login()
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.loggedInRole)
    }

    // ── LoginViewModel — API error mapping ──────────────────────────────────

    @Test
    fun `suspended account shows friendly error message`() {
        val repo = fakeRepo(loginResult = Resource.Error("Account has been suspended by administrator"))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("Password1!")
        vm.login()
        val error = vm.state.value.error
        assertNotNull(error)
        assertTrue("Expected 'suspended' in: $error", error!!.contains("suspended", ignoreCase = true))
    }

    @Test
    fun `unverified email shows verification prompt`() {
        val repo = fakeRepo(loginResult = Resource.Error("Email address not verified"))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("Password1!")
        vm.login()
        val error = vm.state.value.error
        assertNotNull(error)
        assertTrue("Expected 'verify' in: $error", error!!.contains("verify", ignoreCase = true))
    }

    @Test
    fun `invalid credentials maps to friendly message`() {
        val repo = fakeRepo(loginResult = Resource.Error("Invalid email or password"))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("wrongpass")
        vm.login()
        val error = vm.state.value.error
        assertNotNull(error)
        assertTrue("Expected 'Incorrect' in: $error", error!!.contains("Incorrect", ignoreCase = true))
    }

    @Test
    fun `onEmailChange clears existing error`() {
        val repo = fakeRepo(loginResult = Resource.Error("Invalid email or password"))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("wrongpass")
        vm.login()
        assertNotNull(vm.state.value.error)
        // Typing a new email should clear the error
        vm.onEmailChange("new@example.com")
        assertNull("Error should be cleared on field change", vm.state.value.error)
    }

    @Test
    fun `onPasswordChange clears existing error`() {
        val repo = fakeRepo(loginResult = Resource.Error("Invalid email or password"))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("user@example.com")
        vm.onPasswordChange("wrongpass")
        vm.login()
        assertNotNull(vm.state.value.error)
        vm.onPasswordChange("newpass")
        assertNull("Error should be cleared on field change", vm.state.value.error)
    }

    // ── RegisterViewModel — validation rules ─────────────────────────────────

    @Test
    fun `register with blank name shows error`() {
        val vm = RegisterViewModel(fakeRepo())
        vm.update { it.copy(fullName = "", email = "a@b.com", phone = "0821234567", password = "Password1!", confirmPassword = "Password1!") }
        vm.register()
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `register with invalid email shows error`() {
        val vm = RegisterViewModel(fakeRepo())
        vm.update { it.copy(fullName = "Alice Dlamini", email = "not-an-email", phone = "0821234567", password = "Password1!", confirmPassword = "Password1!") }
        vm.register()
        val error = vm.state.value.error
        assertNotNull(error)
        assertTrue("Expected email-related error, got: $error", error!!.contains("email", ignoreCase = true))
    }

    @Test
    fun `register with short password shows character-count error`() {
        val vm = RegisterViewModel(fakeRepo())
        vm.update { it.copy(fullName = "Alice", email = "a@b.com", phone = "082", password = "short", confirmPassword = "short") }
        vm.register()
        val error = vm.state.value.error
        assertNotNull(error)
        assertTrue("Expected 8-character error, got: $error", error!!.contains("8", ignoreCase = true))
    }

    @Test
    fun `register with mismatched passwords shows mismatch error`() {
        val vm = RegisterViewModel(fakeRepo())
        vm.update { it.copy(fullName = "Alice", email = "a@b.com", phone = "082", password = "Password1!", confirmPassword = "Different1!") }
        vm.register()
        val error = vm.state.value.error
        assertNotNull(error)
        assertTrue("Expected 'match' in: $error", error!!.contains("match", ignoreCase = true))
    }

    @Test
    fun `nanny register with invalid hourly rate shows error`() {
        val vm = RegisterViewModel(fakeRepo())
        vm.update { it.copy(
            role = UserRole.NANNY,
            fullName = "Bob", email = "b@b.com", phone = "082",
            password = "Password1!", confirmPassword = "Password1!",
            hourlyRate = "notANumber",
        ) }
        vm.register()
        assertNotNull("Nanny with bad hourly rate should fail", vm.state.value.error)
    }
}

