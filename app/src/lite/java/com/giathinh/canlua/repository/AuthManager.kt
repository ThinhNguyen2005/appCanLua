package com.giathinh.canlua.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor() {
    val currentUser: Any? = null
    val isAuthenticated: Boolean = false
    val authStateFlow: Flow<Any?> = flowOf(null)
}