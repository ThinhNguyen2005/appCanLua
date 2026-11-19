package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.model.Profile
import com.giathinh.canlua.repository.AuthManager
import com.giathinh.canlua.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authManager = mockk<AuthManager>(relaxed = true)
    private val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)
    private val authStateFlow = MutableStateFlow<FirebaseUser?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { authManager.authStateFlow } returns authStateFlow
        every { authManager.currentUser } answers { authStateFlow.value }
        every { authManager.isAuthenticated } answers { authStateFlow.value != null }
        coEvery { profileRepository.ensureCurrentProfile() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeUser(uid: String = "uid-1", email: String? = "nongdan@canlua.vn"): FirebaseUser =
        mockk(relaxed = true) {
            every { this@mockk.uid } returns uid
            every { this@mockk.email } returns email
            every { phoneNumber } returns null
        }

    private fun createViewModel(): AuthViewModel = AuthViewModel(
        authManager = authManager,
        firebaseAuth = firebaseAuth,
        profileRepository = profileRepository
    )

    @Test
    fun `looksLikePhone validates Vietnamese phone patterns vs emails and short strings`() {
        val vm = createViewModel()
        val cases = listOf(
            "0912345678" to true,
            "+84912345678" to true,
            "091 234 5678" to true,
            "user@gmail.com" to false,
            "12345" to false,
            "abc12345678" to false,
            "" to false
        )

        cases.forEach { (input, expected) ->
            assertEquals("Mismatch for '$input'", expected, vm.looksLikePhone(input))
        }
    }

    @Test
    fun `signInEmail updates signed-in state on success and maps friendly error messages on failure`() =
        runTest(testDispatcher) {
            val user = fakeUser(email = "ba@canlua.vn")
            coEvery { profileRepository.ensureCurrentProfile() } returns Profile(uid = "uid-1", name = "Chú Ba")
            coEvery { authManager.signInWithEmailAndPassword("ba@canlua.vn", "123456") } answers {
                authStateFlow.value = user
                Result.success(user)
            }

            val vm = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isSignedIn)

            vm.signInEmail("ba@canlua.vn", "123456")
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isSignedIn)
            assertFalse(vm.uiState.value.loading)
            assertNull(vm.uiState.value.error)
            assertEquals("ba@canlua.vn", vm.uiState.value.userLabel)
            assertEquals(false, vm.uiState.value.needsProfileSetup)

            // Verify error mapping table when signed out
            authStateFlow.value = null
            advanceUntilIdle()

            val errorCases = listOf(
                "The supplied auth credential is incorrect, malformed or has expired." to
                    "Email hoặc mật khẩu không chính xác.",
                "There is no user record corresponding to this email." to
                    "Tài khoản không tồn tại. Vui lòng đăng ký.",
                "The email address is badly formatted." to
                    "Định dạng email không hợp lệ.",
                "Custom failure" to "Custom failure"
            )
            errorCases.forEach { (rawMsg, expectedUiMsg) ->
                coEvery { authManager.signInWithEmailAndPassword(any(), any()) } returns
                    Result.failure(RuntimeException(rawMsg))
                vm.signInEmail("bad@canlua.vn", "wrong")
                advanceUntilIdle()
                assertFalse(vm.uiState.value.loading)
                assertEquals(expectedUiMsg, vm.uiState.value.error)
            }
        }

    @Test
    fun `registerEmail saves profile and maps registration errors accurately`() = runTest(testDispatcher) {
        val user = fakeUser(email = "moi@canlua.vn")
        coEvery { authManager.createUserWithEmailAndPassword("moi@canlua.vn", "secret12") } answers {
            authStateFlow.value = user
            Result.success(user)
        }
        coEvery { profileRepository.saveProfile(any()) } returns true
        coEvery { profileRepository.ensureCurrentProfile() } returns Profile(uid = "uid-1", name = "Cô Tư")

        val vm = createViewModel()
        vm.registerEmail(email = "moi@canlua.vn", password = "secret12", name = "Cô Tư", region = "Long An")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSignedIn)
        assertEquals(false, vm.uiState.value.needsProfileSetup)
        coVerify(exactly = 1) {
            profileRepository.saveProfile(
                match { it.name == "Cô Tư" && it.region == "Long An" && it.email == "moi@canlua.vn" }
            )
        }

        authStateFlow.value = null
        advanceUntilIdle()
        val regErrors = listOf(
            "The email address is already in use by another account." to
                "Email này đã được sử dụng. Vui lòng đăng nhập.",
            "The given password is invalid. Password should be at least 6 characters" to
                "Mật khẩu phải có ít nhất 6 ký tự.",
            "The email address is badly formatted." to
                "Định dạng email không hợp lệ."
        )
        regErrors.forEach { (rawMsg, expectedUiMsg) ->
            coEvery { authManager.createUserWithEmailAndPassword(any(), any()) } returns
                Result.failure(RuntimeException(rawMsg))
            vm.registerEmail("dup@canlua.vn", "123")
            advanceUntilIdle()
            assertEquals(expectedUiMsg, vm.uiState.value.error)
        }
    }

    @Test
    fun `signInAnonymously, needsProfileSetup transition, and signOut update AuthUiState cleanly`() =
        runTest(testDispatcher) {
            val anonUser = fakeUser(uid = "anon-1", email = null)
            coEvery { authManager.signInAnonymously() } answers {
                authStateFlow.value = anonUser
                Result.success(anonUser)
            }
            coEvery { profileRepository.ensureCurrentProfile() } returns Profile(uid = "anon-1", name = "")

            val vm = createViewModel()
            vm.signInAnonymously()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isSignedIn)
            assertEquals("Đăng nhập ẩn danh", vm.uiState.value.info)
            assertEquals(true, vm.uiState.value.needsProfileSetup)

            // User completes profile setup -> markProfileCompleted flips flag to false
            coEvery { profileRepository.ensureCurrentProfile() } returns Profile(uid = "anon-1", name = "Bác Sáu")
            vm.markProfileCompleted()
            advanceUntilIdle()
            assertEquals(false, vm.uiState.value.needsProfileSetup)

            // Sign out clears signed-in state
            every { authManager.signOut() } answers { authStateFlow.value = null }
            vm.signOut()
            advanceUntilIdle()

            verify(exactly = 1) { authManager.signOut() }
            assertFalse(vm.uiState.value.isSignedIn)
            assertEquals("Đã đăng xuất", vm.uiState.value.info)
            assertEquals(false, vm.uiState.value.needsProfileSetup)
        }
}
