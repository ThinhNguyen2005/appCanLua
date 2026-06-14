package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.repository.SyncableCardRepository
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.util.RiceCalculator
import com.GiaThinh.canlua.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WeightInputViewModel @Inject constructor(
    private val repository: SyncableCardRepository,
    private val ttsManager: TextToSpeechManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentCard = MutableStateFlow<Card?>(null)
    val currentCard: StateFlow<Card?> = _currentCard.asStateFlow()

    private val _weightEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()

    private val _weightInputState = MutableStateFlow(WeightInputUiState())
    val weightInputState: StateFlow<WeightInputUiState> = _weightInputState.asStateFlow()

    private val _manualTableCount = MutableStateFlow(0)
    val manualTableCount: StateFlow<Int> = _manualTableCount.asStateFlow()

    val tables: StateFlow<List<List<List<Double?>>>> =
        combine(_weightEntries, _manualTableCount) { entries, count ->
            withContext(Dispatchers.Default) {
                organizeIntoTables(entries, count)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var weightEntriesJob: Job? = null

    private val ttsEnabledState: StateFlow<Boolean> = settingsRepository.ttsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.isTtsEnabled())

    val weighDefaults = settingsRepository.weighDefaults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.getWeighDefaults())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.weighDefaults.collect { defaults ->
                val card = _currentCard.value ?: return@collect
                val computedBagWeight = if (defaults.bagMethodIsSampling && defaults.bagSampleCount > 0) {
                    defaults.bagSampleTotalWeight / defaults.bagSampleCount
                } else if (defaults.bagSampleCount > 0) {
                    1.0 / defaults.bagSampleCount
                } else {
                    1.0 / 8.0
                }
                if (card.weightInputMode != defaults.weightInputMode ||
                    card.bagMethodIsSampling != defaults.bagMethodIsSampling ||
                    card.bagSampleCount != defaults.bagSampleCount ||
                    card.bagSampleTotalWeight != defaults.bagSampleTotalWeight ||
                    card.bagWeight != computedBagWeight
                ) {
                    val updated = card.copy(
                        weightInputMode = defaults.weightInputMode,
                        bagMethodIsSampling = defaults.bagMethodIsSampling,
                        bagSampleCount = defaults.bagSampleCount,
                        bagSampleTotalWeight = defaults.bagSampleTotalWeight,
                        bagWeight = computedBagWeight
                    )
                    repository.updateCard(updated)
                    repository.updateCardCalculations(card.id)
                    _currentCard.value = repository.getCardById(card.id)
                }
            }
        }
    }

    fun loadCardById(cardId: Long) {
        weightEntriesJob?.cancel()
        weightEntriesJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _manualTableCount.value = 0

            var card = repository.getCardById(cardId)
            val defaults = settingsRepository.getWeighDefaults()
            if (card != null && card.weightInputMode != defaults.weightInputMode) {
                val updated = card.copy(weightInputMode = defaults.weightInputMode)
                repository.updateCard(updated)
                card = repository.getCardById(cardId)
            }
            _currentCard.value = card
            syncMoistureToInputState(card)

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

    fun incrementManualTableCount() {
        _manualTableCount.value = _manualTableCount.value + 1
    }

    fun startTts() {
        viewModelScope.launch {
            ttsManager.setEnabled(ttsEnabledState.value)
        }
    }

    fun addWeightEntry(
        cardId: Long,
        weight: Double,
        bagWeight: Double = 0.0,
        impurityWeight: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            val moisture = card?.moisturePercent ?: 0.0

            val netWeight = RiceCalculator.calcNetWeight(
                rawWeight = weight,
                bagWeight = bagWeight,
                impurityWeight = 0.0,
                moisturePercent = moisture
            )

            val weightEntry = WeightEntry(
                cardId = cardId,
                weight = weight,
                bagWeight = bagWeight,
                impurityWeight = 0.0,
                netWeight = netWeight
            )
            val currentSize = _weightEntries.value.size
            val isColumnCompleted = (currentSize % 5 == 4)

            repository.insertWeightEntry(weightEntry)
            repository.updateCardCalculations(cardId)
            _currentCard.value = repository.getCardById(cardId)

            val ttsEnabled = ttsEnabledState.value
            ttsManager.setEnabled(ttsEnabled)
            if (ttsEnabled && ttsManager.isEnabled()) {
                ttsManager.speakNumber(weight, completesColumn = isColumnCompleted)
            } else if (isColumnCompleted) {
                ttsManager.triggerColumnCompleteFeedback()
            }
        }
    }

    fun addWeightEntryDirectly(cardId: Long, weight: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                val netWeight = RiceCalculator.calcNetWeight(
                    rawWeight = weight,
                    bagWeight = it.bagWeight,
                    impurityWeight = 0.0,
                    moisturePercent = it.moisturePercent
                )
                val weightEntry = WeightEntry(
                    cardId = cardId,
                    weight = weight,
                    bagWeight = it.bagWeight,
                    impurityWeight = 0.0,
                    netWeight = netWeight
                )
                val currentSize = _weightEntries.value.size
                val isColumnCompleted = (currentSize % 5 == 4)

                repository.insertWeightEntry(weightEntry)
                repository.updateCardCalculations(cardId)
                _currentCard.value = repository.getCardById(cardId)

                val ttsEnabled = ttsEnabledState.value
                ttsManager.setEnabled(ttsEnabled)
                if (ttsEnabled && ttsManager.isEnabled()) {
                    ttsManager.speakNumber(weight, completesColumn = isColumnCompleted)
                } else if (isColumnCompleted) {
                    ttsManager.triggerColumnCompleteFeedback()
                }
            }
        }
    }

    fun updateWeightEntry(weightEntry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWeightEntry(weightEntry)
            repository.updateCardCalculations(weightEntry.cardId)
            _currentCard.value = repository.getCardById(weightEntry.cardId)
        }
    }

    fun deleteWeightEntry(weightEntry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWeightEntry(weightEntry)
            repository.updateCardCalculations(weightEntry.cardId)
            _currentCard.value = repository.getCardById(weightEntry.cardId)
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
        if (digit == "." && current.contains(".")) return
        if (current.contains(".")) {
            val decimalPart = current.substringAfter(".")
            if (decimalPart.length >= 2 && digit != ".") return
        }
        _weightInputState.value = _weightInputState.value.copy(
            currentWeight = current + digit
        )

        viewModelScope.launch(Dispatchers.Default) {
            val ttsEnabled = ttsEnabledState.value
            ttsManager.setEnabled(ttsEnabled)
            if (ttsEnabled && ttsManager.isEnabled()) {
                ttsManager.speak(digit)
            }
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

    fun updateCardBagWeight(cardId: Long, bagWeight: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(bagWeight = bagWeight)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                _currentCard.value = repository.getCardById(cardId)
            }
        }
    }

    fun updateCardBagMethod(cardId: Long, isSampling: Boolean, sampleCount: Int, sampleTotalWeight: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                val computedBagWeight = if (isSampling && sampleCount > 0) {
                    sampleTotalWeight / sampleCount
                } else if (sampleCount > 0) {
                    1.0 / sampleCount
                } else {
                    1.0 / 8.0
                }
                val updatedCard = it.copy(
                    bagMethodIsSampling = isSampling,
                    bagSampleCount = sampleCount,
                    bagSampleTotalWeight = sampleTotalWeight,
                    bagWeight = computedBagWeight
                )
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                _currentCard.value = repository.getCardById(cardId)
            }
        }
    }

    fun updateCardImpurityWeight(cardId: Long, impurityWeight: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(impurityWeight = impurityWeight)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                _currentCard.value = repository.getCardById(cardId)
            }
        }
    }

    fun updateCardPricePerKg(cardId: Long, pricePerKg: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(pricePerKg = pricePerKg)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                _currentCard.value = repository.getCardById(cardId)
            }
        }
    }

    fun updateCardMoisture(cardId: Long, moisturePercent: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(moisturePercent = moisturePercent)
                repository.updateCard(updatedCard)
                repository.updateCardCalculations(cardId)
                val refreshed = repository.getCardById(cardId)
                _currentCard.value = refreshed
                syncMoistureToInputState(refreshed)
            }
        }
    }

    fun toggleCardLock(cardId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                val updatedCard = it.copy(isLocked = !it.isLocked)
                repository.updateCard(updatedCard)
                _currentCard.value = repository.getCardById(cardId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        weightEntriesJob?.cancel()
        ttsManager.shutdown()
    }

    /** Giữ [moisturePercent] trong [_weightInputState] đồng bộ với card hiện tại. */
    private fun syncMoistureToInputState(card: Card?) {
        val moisture = card?.moisturePercent ?: 0.0
        if (_weightInputState.value.moisturePercent != moisture) {
            _weightInputState.value = _weightInputState.value.copy(moisturePercent = moisture)
        }
    }
}

private fun organizeIntoTables(entries: List<WeightEntry>, manualCount: Int): List<List<List<Double?>>> {
    val totalEntries = entries.size
    val calculatedNumTables = (totalEntries / 25) + 1
    val numTables = (calculatedNumTables + manualCount).coerceAtLeast(1)

    val tables = mutableListOf<List<List<Double?>>>()

    for (t in 0 until numTables) {
        val tableGrid = MutableList(5) { MutableList<Double?>(5) { null } }
        for (c in 0 until 5) {
            for (r in 0 until 5) {
                val entryIdx = (t * 25) + (c * 5) + r
                if (entryIdx < totalEntries) {
                    tableGrid[r][c] = entries[entryIdx].weight
                }
            }
        }
        tables.add(tableGrid.map { it.toList() })
    }
    return tables
}
