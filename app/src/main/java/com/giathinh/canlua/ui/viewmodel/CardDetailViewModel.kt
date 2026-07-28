package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.data.location.LocationProvider
import com.giathinh.canlua.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: CardRepository,
    private val locationProvider: LocationProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _currentCard = MutableStateFlow<Card?>(null)
    val currentCard: StateFlow<Card?> = _currentCard.asStateFlow()

    private val _weightEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var weightEntriesJob: Job? = null

    fun loadCardById(cardId: Long) {
        weightEntriesJob?.cancel()
        weightEntriesJob = viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            val card = repository.getCardById(cardId)
            _currentCard.value = card

            if (card != null) {
                repository.getWeightEntriesByCardId(cardId).collect { entries ->
                    _weightEntries.value = entries
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
            }
        }
    }

    fun updateCard(card: Card) {
        val current = _currentCard.value
        if (current != null && current.id == card.id) {
            _currentCard.value = card
        }
        viewModelScope.launch(ioDispatcher) {
            val oldCard = repository.getCardById(card.id)
            repository.updateCard(card)

            var needRecalc = false
            if (oldCard != null) {
                val depositDiff = card.depositAmount - oldCard.depositAmount
                if (depositDiff != 0.0) {
                    repository.insertTransaction(
                        com.giathinh.canlua.data.model.Transaction(
                            cardId = card.id,
                            amount = depositDiff,
                            type = com.giathinh.canlua.data.model.TransactionType.DEPOSIT,
                            description = "Cập nhật tiền cọc"
                        )
                    )
                    needRecalc = true
                }

                val paidDiff = card.paidAmount - oldCard.paidAmount
                if (paidDiff != 0.0) {
                    repository.insertTransaction(
                        com.giathinh.canlua.data.model.Transaction(
                            cardId = card.id,
                            amount = paidDiff,
                            type = com.giathinh.canlua.data.model.TransactionType.PAYMENT,
                            description = "Cập nhật đã trả"
                        )
                    )
                    needRecalc = true
                }
            }

            if (needRecalc || 
                oldCard?.pricePerKg != card.pricePerKg ||
                oldCard?.impurityWeight != card.impurityWeight ||
                oldCard?.moisturePercent != card.moisturePercent
            ) {
                repository.updateCardCalculations(card.id)
                loadCardById(card.id)
            } else {
                val latestCard = repository.getCardById(card.id)
                if (latestCard != null) {
                    _currentCard.value = latestCard
                }
            }
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch(ioDispatcher) {
            repository.deleteCard(card)
        }
    }

    fun updateCardName(cardId: Long, name: String) {
        viewModelScope.launch(ioDispatcher) {
            if (name.isBlank()) return@launch
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(name = name.trim()))
            }
        }
    }

    fun updateCardBagWeight(cardId: Long, bagWeight: Double) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(bagWeight = bagWeight)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                loadCardById(cardId)
            }
        }
    }

    fun updateCardImpurityWeight(cardId: Long, impurityWeight: Double) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(impurityWeight = impurityWeight)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                loadCardById(cardId)
            }
        }
    }

    fun updateCardPricePerKg(cardId: Long, pricePerKg: Double) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(pricePerKg = pricePerKg)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                loadCardById(cardId)
            }
        }
    }

    fun updateCardMoisture(cardId: Long, moisturePercent: Double) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(moisturePercent = moisturePercent)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                loadCardById(cardId)
            }
        }
    }

    fun updateCardRiceVariety(cardId: Long, variety: String) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(riceVariety = variety))
                loadCardById(cardId)
            }
        }
    }

    fun updateCardSeasonLabel(cardId: Long, seasonLabel: String) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(seasonLabel = seasonLabel))
                loadCardById(cardId)
            }
        }
    }

    fun updateCardTraderPhone(cardId: Long, phone: String) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(traderPhone = phone.trim()))
                loadCardById(cardId)
            }
        }
    }

    fun updateCardFieldAddress(cardId: Long, address: String) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(fieldAddress = address.trim()))
                loadCardById(cardId)
            }
        }
    }

    fun refreshFieldLocation(cardId: Long) {
        viewModelScope.launch(ioDispatcher) {
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
            loadCardById(cardId)
        }
    }

    fun addPayment(cardId: Long, amount: Double, description: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            repository.insertTransaction(
                com.giathinh.canlua.data.model.Transaction(
                    cardId = cardId,
                    amount = amount,
                    type = com.giathinh.canlua.data.model.TransactionType.PAYMENT,
                    description = description
                )
            )
            repository.updateCardCalculations(cardId)
        }
    }

    fun toggleCardLock(cardId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(isLocked = !it.isLocked)
                repository.updateCard(updatedCard)
                loadCardById(cardId)
            }
        }
    }

    fun updateCardWeighOptions(
        cardId: Long,
        impurityIsPercent: Boolean,
        bagMethodIsSampling: Boolean,
        bagSampleCount: Int,
        bagSampleTotalWeight: Double,
        weightInputMode: String
    ) {
        viewModelScope.launch(ioDispatcher) {
            val card = repository.getCardById(cardId) ?: return@launch
            val updated = card.copy(
                impurityIsPercent = impurityIsPercent,
                bagMethodIsSampling = bagMethodIsSampling,
                bagSampleCount = bagSampleCount,
                bagSampleTotalWeight = bagSampleTotalWeight,
                weightInputMode = weightInputMode
            )
            repository.updateCard(updated)
            repository.updateCardCalculations(cardId)
            loadCardById(cardId)
        }
    }

    fun updateWeightEntry(entry: WeightEntry) {
        viewModelScope.launch(ioDispatcher) {
            repository.updateWeightEntry(entry)
            repository.updateCardCalculations(entry.cardId)
            loadCardById(entry.cardId)
        }
    }

    fun deleteWeightEntry(entry: WeightEntry) {
        viewModelScope.launch(ioDispatcher) {
            repository.deleteWeightEntry(entry)
            repository.updateCardCalculations(entry.cardId)
            loadCardById(entry.cardId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        weightEntriesJob?.cancel()
    }
}
