package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.repository.CardRepository
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository: CardRepository,
    private val ttsManager: TextToSpeechManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _currentCard = MutableStateFlow<Card?>(null)
    val currentCard: StateFlow<Card?> = _currentCard.asStateFlow()

    private val _weightEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()

    private val _weightInputState = MutableStateFlow(WeightInputUiState())
    val weightInputState: StateFlow<WeightInputUiState> = _weightInputState.asStateFlow()

    init {
        loadCards()
        ttsManager.initialize()
        ttsManager.setEnabled(settingsRepository.isTtsEnabled())
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
            // Validation: tên không được bỏ trống
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return@launch
            }
            val newCard = Card(
                name = trimmedName,
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
            
            // Speak final weight if TTS enabled
            val ttsEnabled = settingsRepository.isTtsEnabled()
            ttsManager.setEnabled(ttsEnabled)
            if (ttsEnabled && ttsManager.isEnabled()) {
                ttsManager.speakNumber(weight)
            }
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
                // Reload card to update UI
                loadCardById(cardId)
            }
        }
    }

    fun updateCurrentWeight(weight: String) {
        _weightInputState.value = _weightInputState.value.copy(currentWeight = weight)
    }

    fun updateBagWeight(weight: String) {
        _weightInputState.value = _weightInputState.value.copy(bagWeight = weight)
    }

    fun updateImpurityWeight(weight: String) {
        _weightInputState.value = _weightInputState.value.copy(impurityWeight = weight)
    }

    fun toggleLock() {
        _weightInputState.value = _weightInputState.value.copy(
            isLocked = !_weightInputState.value.isLocked
        )
    }

    fun clearWeightInput() {
        _weightInputState.value = _weightInputState.value.copy(currentWeight = "")
    }

    fun appendToWeight(digit: String) {
        val current = _weightInputState.value.currentWeight
        // Validation: không cho phép nhiều dấu chấm
        if (digit == "." && current.contains(".")) return
        // Giới hạn số chữ số thập phân
        if (current.contains(".")) {
            val decimalPart = current.substringAfter(".")
            if (decimalPart.length >= 2 && digit != ".") return
        }
        _weightInputState.value = _weightInputState.value.copy(
            currentWeight = current + digit
        )
        
        // Speak digit if TTS enabled
        val ttsEnabled = settingsRepository.isTtsEnabled()
        ttsManager.setEnabled(ttsEnabled)
        if (ttsEnabled && ttsManager.isEnabled()) {
            ttsManager.speak(digit)
        }
    }

    fun removeLastDigit() {
        val current = _weightInputState.value.currentWeight
        if (current.isNotEmpty()) {
            _weightInputState.value = _weightInputState.value.copy(
                currentWeight = current.dropLast(1)
            )
        }
    }

    fun updateCardName(cardId: Long, name: String) {
        viewModelScope.launch {
            // Validation: tên không được bỏ trống
            if (name.isBlank()) {
                return@launch
            }
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(name = name.trim()))
            }
        }
    }

    fun updateCardBagWeight(cardId: Long, bagWeight: Double) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(bagWeight = bagWeight)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun updateCardImpurityWeight(cardId: Long, impurityWeight: Double) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(impurityWeight = impurityWeight)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun updateCardPricePerKg(cardId: Long, pricePerKg: Double) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(pricePerKg = pricePerKg)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun updateCardDepositAmount(cardId: Long, depositAmount: Double) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(depositAmount = depositAmount))
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun updateCardPaidAmount(cardId: Long, paidAmount: Double) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(paidAmount = paidAmount))
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun addWeightEntryDirectly(cardId: Long, weight: Double) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                // Sử dụng bagWeight và impurityWeight từ card
                val netWeight = weight - it.bagWeight - it.impurityWeight
                val weightEntry = WeightEntry(
                    cardId = cardId,
                    weight = weight,
                    bagWeight = it.bagWeight,
                    impurityWeight = it.impurityWeight,
                    netWeight = netWeight
                )
                repository.insertWeightEntry(weightEntry)
                
                // Update card calculations
                repository.updateCardCalculations(cardId)
                
                // Speak final weight if TTS enabled
                val ttsEnabled = settingsRepository.isTtsEnabled()
                ttsManager.setEnabled(ttsEnabled)
                if (ttsEnabled && ttsManager.isEnabled()) {
                    ttsManager.speakNumber(weight)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

