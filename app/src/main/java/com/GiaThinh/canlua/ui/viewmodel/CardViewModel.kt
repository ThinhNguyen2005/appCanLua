package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _currentCard = MutableStateFlow<Card?>(null)
    val currentCard: StateFlow<Card?> = _currentCard.asStateFlow()

    private val _weightEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            repository.getAllCards().collect { cardsList ->
                _cards.value = cardsList
            }
        }
    }

    fun loadCardById(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            _currentCard.value = card
            
            if (card != null) {
                repository.getWeightEntriesByCardId(cardId).collect { entries ->
                    _weightEntries.value = entries
                }
            }
        }
    }

    fun createNewCard(
        name: String,
        cccd: String?,
        pricePerKg: Double,
        depositAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            val newCard = Card(
                name = name,
                cccd = cccd,
                date = Date(),
                pricePerKg = pricePerKg,
                depositAmount = depositAmount
            )
            val cardId = repository.insertCard(newCard)
            
            // Insert deposit transaction if amount > 0
            if (depositAmount > 0) {
                repository.insertTransaction(
                    com.GiaThinh.canlua.data.model.Transaction(
                        cardId = cardId,
                        amount = depositAmount,
                        type = com.GiaThinh.canlua.data.model.TransactionType.DEPOSIT,
                        description = "Tiền cọc"
                    )
                )
            }
        }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch {
            repository.updateCard(card)
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun addWeightEntry(
        cardId: Long,
        weight: Double,
        bagWeight: Double = 0.0,
        impurityWeight: Double = 0.0
    ) {
        viewModelScope.launch {
            val netWeight = weight - bagWeight - impurityWeight
            val weightEntry = WeightEntry(
                cardId = cardId,
                weight = weight,
                bagWeight = bagWeight,
                impurityWeight = impurityWeight,
                netWeight = netWeight
            )
            repository.insertWeightEntry(weightEntry)
            
            // Update card calculations
            repository.updateCardCalculations(cardId)
        }
    }

    fun updateWeightEntry(weightEntry: WeightEntry) {
        viewModelScope.launch {
            repository.updateWeightEntry(weightEntry)
            
            // Update card calculations
            repository.updateCardCalculations(weightEntry.cardId)
        }
    }

    fun deleteWeightEntry(weightEntry: WeightEntry) {
        viewModelScope.launch {
            repository.deleteWeightEntry(weightEntry)
            
            // Update card calculations
            repository.updateCardCalculations(weightEntry.cardId)
        }
    }

    fun addPayment(cardId: Long, amount: Double, description: String? = null) {
        viewModelScope.launch {
            repository.insertTransaction(
                com.GiaThinh.canlua.data.model.Transaction(
                    cardId = cardId,
                    amount = amount,
                    type = com.GiaThinh.canlua.data.model.TransactionType.PAYMENT,
                    description = description
                )
            )
            
            // Update card calculations
            repository.updateCardCalculations(cardId)
        }
    }

    fun toggleCardLock(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(isLocked = !it.isLocked)
                repository.updateCard(updatedCard)
            }
        }
    }
}

