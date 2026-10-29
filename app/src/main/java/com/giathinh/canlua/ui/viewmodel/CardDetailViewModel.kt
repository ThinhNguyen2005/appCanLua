package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.location.LocationProvider
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.SyncableCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: SyncableCardRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _cardId = MutableStateFlow<Long?>(null)

    val currentCard: StateFlow<Card?> = _cardId
        .flatMapLatest { id ->
            if (id == null || id <= 0L) flowOf(null)
            else repository.getCardByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val weightEntries: StateFlow<List<WeightEntry>> = _cardId
        .flatMapLatest { id ->
            if (id == null || id <= 0L) flowOf(emptyList())
            else repository.getWeightEntriesByCardId(id)
        }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadCardById(cardId: Long) {
        if (_cardId.value != cardId) {
            _isLoading.value = true
            _cardId.value = cardId
        } else {
            _isLoading.value = false
        }
    }

    private fun updateCardField(cardId: Long, recalculate: Boolean = true, transform: (Card) -> Card) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId) ?: return@launch
            repository.updateCard(transform(card))
            if (recalculate) {
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldCard = repository.getCardById(card.id) ?: return@launch
            repository.updateCard(card)

            var needRecalc = false
            val depositDiff = card.depositAmount - oldCard.depositAmount
            if (depositDiff != 0.0) {
                repository.insertTransaction(
                    Transaction(
                        cardId = card.id,
                        amount = depositDiff,
                        type = TransactionType.DEPOSIT,
                        description = "Cập nhật tiền cọc"
                    )
                )
                needRecalc = true
            }

            val paidDiff = card.paidAmount - oldCard.paidAmount
            if (paidDiff != 0.0) {
                repository.insertTransaction(
                    Transaction(
                        cardId = card.id,
                        amount = paidDiff,
                        type = TransactionType.PAYMENT,
                        description = "Cập nhật đã trả"
                    )
                )
                needRecalc = true
            }

            if (needRecalc ||
                oldCard.pricePerKg != card.pricePerKg ||
                oldCard.impurityWeight != card.impurityWeight ||
                oldCard.moisturePercent != card.moisturePercent
            ) {
                repository.updateCardCalculations(card.id)
            }
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCard(card)
        }
    }

    fun updateCardName(cardId: Long, name: String) {
        if (name.isBlank()) return
        updateCardField(cardId, recalculate = false) { it.copy(name = name.trim()) }
    }

    fun updateCardBagWeight(cardId: Long, bagWeight: Double) =
        updateCardField(cardId) { it.copy(bagWeight = bagWeight) }

    fun updateCardImpurityWeight(cardId: Long, impurityWeight: Double) =
        updateCardField(cardId) { it.copy(impurityWeight = impurityWeight) }

    fun updateCardPricePerKg(cardId: Long, pricePerKg: Double) =
        updateCardField(cardId) { it.copy(pricePerKg = pricePerKg) }

    fun updateCardMoisture(cardId: Long, moisturePercent: Double) =
        updateCardField(cardId) { it.copy(moisturePercent = moisturePercent) }

    fun updateCardRiceVariety(cardId: Long, variety: String) =
        updateCardField(cardId, recalculate = false) { it.copy(riceVariety = variety) }

    fun updateCardSeasonLabel(cardId: Long, seasonLabel: String) =
        updateCardField(cardId, recalculate = false) { it.copy(seasonLabel = seasonLabel) }

    fun updateCardTraderPhone(cardId: Long, phone: String) =
        updateCardField(cardId, recalculate = false) { it.copy(traderPhone = phone.trim()) }

    fun updateCardFieldAddress(cardId: Long, address: String) =
        updateCardField(cardId, recalculate = false) { it.copy(fieldAddress = address.trim()) }

    fun refreshFieldLocation(cardId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val geo = runCatching { locationProvider.getCurrentLocation(forceFresh = true) }.getOrNull() ?: return@launch
            val address = runCatching { locationProvider.reverseGeocode(geo.lat, geo.lon) }.getOrNull().orEmpty()
            val card = repository.getCardById(cardId) ?: return@launch
            repository.updateCard(
                card.copy(
                    latitude = geo.lat,
                    longitude = geo.lon,
                    fieldAddress = address.ifBlank { card.fieldAddress }
                )
            )
        }
    }

    fun addPayment(cardId: Long, amount: Double, description: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId) ?: return@launch
            if (card.isLocked) return@launch
            repository.insertTransaction(
                Transaction(
                    cardId = cardId,
                    amount = amount,
                    type = TransactionType.PAYMENT,
                    description = description ?: "Thanh toán thêm"
                )
            )
            repository.updateCardCalculations(cardId)
        }
    }

    fun toggleCardLock(cardId: Long) =
        updateCardField(cardId, recalculate = false) { it.copy(isLocked = !it.isLocked) }

    fun updateCardWeighOptions(
        cardId: Long,
        impurityIsPercent: Boolean,
        bagMethodIsSampling: Boolean,
        bagSampleCount: Int,
        bagSampleTotalWeight: Double,
        weightInputMode: String
    ) = updateCardField(cardId) {
        it.copy(
            impurityIsPercent = impurityIsPercent,
            bagMethodIsSampling = bagMethodIsSampling,
            bagSampleCount = bagSampleCount,
            bagSampleTotalWeight = bagSampleTotalWeight,
            weightInputMode = weightInputMode
        )
    }

    fun updateWeightEntry(entry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWeightEntry(entry)
            repository.updateCardCalculations(entry.cardId)
        }
    }

    fun deleteWeightEntry(entry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWeightEntry(entry)
            repository.updateCardCalculations(entry.cardId)
        }
    }
}
