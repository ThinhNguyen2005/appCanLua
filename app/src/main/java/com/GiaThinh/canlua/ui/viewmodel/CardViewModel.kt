package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.repository.CardRepository
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.util.RiceCalculator
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Filter state
    private val _selectedVarietyFilter = MutableStateFlow<String?>(null)
    val selectedVarietyFilter: StateFlow<String?> = _selectedVarietyFilter.asStateFlow()

    private val _availableVarieties = MutableStateFlow<List<String>>(emptyList())
    val availableVarieties: StateFlow<List<String>> = _availableVarieties.asStateFlow()

    init {
        loadCards()
        loadAvailableVarieties()
        ttsManager.initialize()
        ttsManager.setEnabled(settingsRepository.isTtsEnabled())
    }

    fun refreshCards() {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllCards().collect { cardsList ->
                _cards.value = cardsList
                _isLoading.value = false
            }
        }
    }

    private fun loadAvailableVarieties() {
        viewModelScope.launch {
            repository.getDistinctRiceVarieties().collect { varieties ->
                _availableVarieties.value = varieties
            }
        }
    }

    fun setVarietyFilter(variety: String?) {
        _selectedVarietyFilter.value = variety
        if (variety != null) {
            viewModelScope.launch {
                repository.getCardsByRiceVariety(variety).collect { filtered ->
                    _cards.value = filtered
                }
            }
        } else {
            loadCards()
        }
    }

    fun loadCardById(cardId: Long) {
        viewModelScope.launch {
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

    fun createNewCard(
        name: String,
        cccd: String?,
        traderName: String,
        pricePerKg: Double,
        depositAmount: Double = 0.0,
        riceVariety: String = "",
        moisturePercent: Double = 0.0,
        seasonLabel: String = ""
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch

            val newCard = Card(
                name = trimmedName,
                cccd = cccd,
                traderName = traderName.trim(),
                date = Date(),
                pricePerKg = pricePerKg,
                depositAmount = depositAmount,
                riceVariety = riceVariety,
                moisturePercent = moisturePercent,
                seasonLabel = seasonLabel
            )
            val cardId = repository.insertCard(newCard)
            
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
            val oldCard = repository.getCardById(card.id)
            repository.updateCard(card)

            var needRecalc = false
            if (oldCard != null) {
                val depositDiff = card.depositAmount - oldCard.depositAmount
                if (depositDiff != 0.0) {
                    repository.insertTransaction(
                        com.GiaThinh.canlua.data.model.Transaction(
                            cardId = card.id,
                            amount = depositDiff,
                            type = com.GiaThinh.canlua.data.model.TransactionType.DEPOSIT,
                            description = "Cập nhật tiền cọc"
                        )
                    )
                    needRecalc = true
                }

                val paidDiff = card.paidAmount - oldCard.paidAmount
                if (paidDiff != 0.0) {
                    repository.insertTransaction(
                        com.GiaThinh.canlua.data.model.Transaction(
                            cardId = card.id,
                            amount = paidDiff,
                            type = com.GiaThinh.canlua.data.model.TransactionType.PAYMENT,
                            description = "Cập nhật đã trả"
                        )
                    )
                    needRecalc = true
                }
            }

            if (needRecalc || oldCard?.pricePerKg != card.pricePerKg) {
                repository.updateCardCalculations(card.id)
                loadCardById(card.id)
            }
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
            val card = repository.getCardById(cardId)
            val moisture = card?.moisturePercent ?: 0.0

            val netWeight = RiceCalculator.calcNetWeight(
                rawWeight = weight,
                bagWeight = bagWeight,
                impurityWeight = impurityWeight,
                moisturePercent = moisture
            )

            val weightEntry = WeightEntry(
                cardId = cardId,
                weight = weight,
                bagWeight = bagWeight,
                impurityWeight = impurityWeight,
                netWeight = netWeight
            )
            repository.insertWeightEntry(weightEntry)
            repository.updateCardCalculations(cardId)
            
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
            repository.updateCardCalculations(weightEntry.cardId)
        }
    }

    fun deleteWeightEntry(weightEntry: WeightEntry) {
        viewModelScope.launch {
            repository.deleteWeightEntry(weightEntry)
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
            repository.updateCardCalculations(cardId)
        }
    }

    fun toggleCardLock(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(isLocked = !it.isLocked)
                repository.updateCard(updatedCard)
                loadCardById(cardId)
            }
        }
    }

    // === QR Handshake ===

    fun generateQrToken(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId) ?: return@launch
            val token = RiceCalculator.generateQrToken(
                cardId = card.id,
                netWeight = card.totalWeight,
                totalAmount = card.totalAmount
            )
            repository.updateQrToken(cardId, token)
            loadCardById(cardId)
        }
    }

    fun verifyAndLockTransaction(scannedToken: String, traderId: String) {
        viewModelScope.launch {
            val card = repository.findByQrToken(scannedToken)
            if (card != null && !card.isLocked) {
                repository.lockCard(cardId = card.id, traderId = traderId)
                _currentCard.value = repository.getCardById(card.id)
            }
        }
    }

    // === Weight input state management ===

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
        if (digit == "." && current.contains(".")) return
        if (current.contains(".")) {
            val decimalPart = current.substringAfter(".")
            if (decimalPart.length >= 2 && digit != ".") return
        }
        _weightInputState.value = _weightInputState.value.copy(
            currentWeight = current + digit
        )
        
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
            if (name.isBlank()) return@launch
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

    fun updateCardMoisture(cardId: Long, moisturePercent: Double) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(moisturePercent = moisturePercent)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun updateCardRiceVariety(cardId: Long, variety: String) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(riceVariety = variety))
            }
        }
    }

    fun updateCardSeasonLabel(cardId: Long, seasonLabel: String) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(seasonLabel = seasonLabel))
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
                val netWeight = RiceCalculator.calcNetWeight(
                    rawWeight = weight,
                    bagWeight = it.bagWeight,
                    impurityWeight = it.impurityWeight,
                    moisturePercent = it.moisturePercent
                )
                val weightEntry = WeightEntry(
                    cardId = cardId,
                    weight = weight,
                    bagWeight = it.bagWeight,
                    impurityWeight = it.impurityWeight,
                    netWeight = netWeight
                )
                repository.insertWeightEntry(weightEntry)
                repository.updateCardCalculations(cardId)
                
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
