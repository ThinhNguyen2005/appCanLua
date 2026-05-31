package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.repository.FirestoreRepository
import com.GiaThinh.canlua.repository.SyncableCardRepository
import com.GiaThinh.canlua.repository.QrLockResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val repository: SyncableCardRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _qrVerificationState = MutableStateFlow<QrVerificationState>(QrVerificationState.Idle)
    val qrVerificationState: StateFlow<QrVerificationState> = _qrVerificationState.asStateFlow()

    fun verifyAndLockTransaction(scannedToken: String, traderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _qrVerificationState.value = QrVerificationState.Loading
            val result = firestoreRepository.lockCardByQrToken(scannedToken, traderId)
            _qrVerificationState.value = when (result) {
                is QrLockResult.Success -> {
                    lockLocalCardIfPresent(scannedToken, traderId)
                    QrVerificationState.Success(result.firestoreId)
                }
                is QrLockResult.AlreadyLockedByCurrentTrader -> {
                    lockLocalCardIfPresent(scannedToken, traderId)
                    QrVerificationState.AlreadyConfirmed(result.firestoreId)
                }
                QrLockResult.NotFound -> QrVerificationState.NotFound
                QrLockResult.AlreadyLockedByOtherTrader -> QrVerificationState.LockedByOtherTrader
                is QrLockResult.Error -> QrVerificationState.Error(result.message)
            }
        }
    }

    fun resetQrVerificationState() {
        _qrVerificationState.value = QrVerificationState.Idle
    }

    private suspend fun lockLocalCardIfPresent(scannedToken: String, traderId: String) {
        val card = repository.findByQrToken(scannedToken)
        if (card != null && !card.isLocked) {
            repository.lockCard(cardId = card.id, traderId = traderId)
        }
    }
}

sealed interface QrVerificationState {
    object Idle : QrVerificationState
    object Loading : QrVerificationState
    data class Success(val firestoreId: String) : QrVerificationState
    data class AlreadyConfirmed(val firestoreId: String) : QrVerificationState
    object NotFound : QrVerificationState
    object LockedByOtherTrader : QrVerificationState
    data class Error(val message: String?) : QrVerificationState
}

