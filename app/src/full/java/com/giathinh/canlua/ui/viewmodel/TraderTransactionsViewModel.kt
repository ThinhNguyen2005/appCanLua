package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.firestore.FirestoreCard
import com.giathinh.canlua.data.firestore.FirestoreTransaction
import com.giathinh.canlua.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/**
 * Filter cho danh sách giao dịch của TRADER.
 */
enum class TransactionFilter(val label: String) {
    ALL("Tất cả"),
    THIS_WEEK("Tuần này"),
    THIS_MONTH("Tháng này"),
    UNPAID("Còn nợ")
}

/**
 * Một giao dịch tổng hợp (1 thẻ + các transaction liên quan).
 */
data class TraderTransactionItem(
    val card: FirestoreCard,
    val transactions: List<FirestoreTransaction>
) {
    val totalPaid: Double
        get() = transactions
            .filter { it.type == "PAYMENT" || it.type == "DEPOSIT" }
            .sumOf { it.amount } - transactions
            .filter { it.type == "REFUND" }
            .sumOf { it.amount }

    val remaining: Double
        get() = (card.totalAmount - totalPaid).coerceAtLeast(0.0)

    val isFullyPaid: Boolean
        get() = remaining < 1.0 && card.totalAmount > 0.0
}

/**
 * UI state tổng hợp cho màn hình Transactions của TRADER.
 */
data class TraderTransactionsUiState(
    val isLoading: Boolean = true,
    val items: List<TraderTransactionItem> = emptyList(),
    val totalCards: Int = 0,
    val totalNetWeight: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val unpaidCount: Int = 0,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TraderTransactionsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter.ALL)
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    /** Stream cards mà trader đã verify (lockedByTraderId == uid). */
    private val cardsFlow = firestoreRepository.observeMyTraderCards()

    /** Stream transactions theo cardIds — flatMapLatest để đổi sub-collection khi card list đổi. */
    private val transactionsFlow = cardsFlow
        .flatMapLatest { cards ->
            val ids = cards.map { it.id }.filter { it.isNotEmpty() }
            if (ids.isEmpty()) flowOf(emptyList())
            else firestoreRepository.observeTransactionsForCards(ids)
        }

    val uiState: StateFlow<TraderTransactionsUiState> =
        combine(cardsFlow, transactionsFlow, _filter) { cards, txs, filter ->
            val filteredCards = applyFilter(cards, txs, filter)
            val items = filteredCards.map { card ->
                TraderTransactionItem(
                    card = card,
                    transactions = txs.filter { it.cardId == card.id }
                )
            }

            // Stats luôn tính trên TẤT CẢ cards (không phụ thuộc filter)
            val allItems = cards.map { card ->
                TraderTransactionItem(
                    card = card,
                    transactions = txs.filter { it.cardId == card.id }
                )
            }
            val totalAmount = allItems.sumOf { it.card.totalAmount }
            val totalPaid = allItems.sumOf { it.totalPaid }

            TraderTransactionsUiState(
                isLoading = false,
                items = items,
                totalCards = cards.size,
                totalNetWeight = allItems.sumOf { it.card.netWeight },
                totalAmount = totalAmount,
                totalPaid = totalPaid,
                totalRemaining = (totalAmount - totalPaid).coerceAtLeast(0.0),
                unpaidCount = allItems.count { !it.isFullyPaid && it.card.totalAmount > 0.0 }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TraderTransactionsUiState(isLoading = true)
        )

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    private fun applyFilter(
        cards: List<FirestoreCard>,
        txs: List<FirestoreTransaction>,
        filter: TransactionFilter
    ): List<FirestoreCard> {
        val now = Calendar.getInstance()
        return when (filter) {
            TransactionFilter.ALL -> cards
            TransactionFilter.THIS_WEEK -> {
                val start = (now.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                cards.filter { it.date.time >= start }
            }
            TransactionFilter.THIS_MONTH -> {
                val start = (now.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                cards.filter { it.date.time >= start }
            }
            TransactionFilter.UNPAID -> cards.filter { card ->
                val paid = txs.filter { it.cardId == card.id && (it.type == "PAYMENT" || it.type == "DEPOSIT") }
                    .sumOf { it.amount }
                val refund = txs.filter { it.cardId == card.id && it.type == "REFUND" }
                    .sumOf { it.amount }
                val remaining = card.totalAmount - (paid - refund)
                remaining > 1.0
            }
        }
    }
}
