package com.giathinh.canlua.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor() {
    val currentUser: FirebaseUser? = null
    val isAuthenticated: Boolean = false
    val authStateFlow: Flow<FirebaseUser?> = flowOf(null)
}