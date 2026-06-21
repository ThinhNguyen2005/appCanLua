package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.DeletedCard
import com.giathinh.canlua.repository.AuthManager
import com.giathinh.canlua.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho `DeletedCardsScreen` — list tombstone + actions:
 *  - Khôi phục: insert lại Card từ JSON snapshot, force push lên cloud.
 *  - Xoá vĩnh viễn: purge tombstone (cloud đã xoá ở `deleteCard` hoặc retry).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeletedCardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val uidFlow = MutableStateFlow(authManager.currentUser?.uid.orEmpty())

    val items: StateFlow<List<DeletedCard>> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList())
            else cardRepository.getDeletedCards(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _restored = MutableStateFlow<Long?>(null)
    /** Emit Room id của card vừa khôi phục để UI navigate vào detail. */
    val restored: StateFlow<Long?> = _restored.asStateFlow()

    init {
        // Refresh uid flow khi auth state đổi (login khác user).
        viewModelScope.launch {
            authManager.authStateFlow.collect { user ->
                uidFlow.value = user?.uid.orEmpty()
            }
        }
    }

    fun restore(tombstoneId: Long) {
        viewModelScope.launch {
            _restored.value = cardRepository.restoreFromTombstone(tombstoneId)
        }
    }

    fun clearRestoredEvent() { _restored.value = null }

    fun purge(tombstoneId: Long) {
        viewModelScope.launch {
            cardRepository.purgeTombstone(tombstoneId)
        }
    }
}
