package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.SyncableCardRepository
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeightInputViewModel @Inject constructor(
    private val repository: SyncableCardRepository,
    private val ttsManager: TextToSpeechManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _cardId = MutableStateFlow<Long?>(null)

    val currentCard: StateFlow<Card?> = _cardId
        .flatMapLatest { id ->
            if (id == null || id <= 0L) flowOf(null)
            else repository.getCardByIdFlow(id)
        }
        .onEach { syncMoistureToInputState(it) }
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

    private val _weightInputState = MutableStateFlow(WeightInputUiState())
    val weightInputState: StateFlow<WeightInputUiState> = _weightInputState.asStateFlow()

    private val _manualTableCount = MutableStateFlow(0)
    val manualTableCount: StateFlow<Int> = _manualTableCount.asStateFlow()

    val tables: StateFlow<List<List<List<Double?>>>> =
        combine(weightEntries, _manualTableCount) { entries, count ->
            withContext(Dispatchers.Default) {
                organizeIntoTables(entries, count)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val ttsEnabledState: StateFlow<Boolean> = settingsRepository.ttsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.isTtsEnabled())

    val weighDefaults = settingsRepository.weighDefaults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.getWeighDefaults())

    fun loadCardById(cardId: Long) {
        if (_cardId.value != cardId) {
            _isLoading.value = true
            _manualTableCount.value = 0
            _cardId.value = cardId
        }
    }

    fun incrementManualTableCount() {
        _manualTableCount.update { it + 1 }
    }

    fun startTts() {
        viewModelScope.launch {
            ttsManager.setEnabled(ttsEnabledState.value)
        }
    }

    private companion object {
        const val MAX_BAG_KG = 200.0
    }

    fun addWeightEntryDirectly(cardId: Long, weight: Double) {
        if (!weight.isFinite() || weight <= 0.0 || weight > MAX_BAG_KG) return
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.addWeightEntryAndRecalc(cardId, weight) ?: return@launch

            val completes = (count % 5 == 0)
            val ttsEnabled = ttsEnabledState.value
            ttsManager.setEnabled(ttsEnabled)
            if (ttsEnabled && ttsManager.isEnabled()) {
                ttsManager.speakNumber(weight, completesColumn = completes)
            } else if (completes) {
                ttsManager.triggerColumnCompleteFeedback()
            }
        }
    }

    fun updateWeightEntry(weightEntry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWeightEntry(weightEntry)
            repository.updateCardCalculations(weightEntry.cardId)
        }
    }

    fun deleteWeightEntry(weightEntry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWeightEntry(weightEntry)
            repository.updateCardCalculations(weightEntry.cardId)
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

    private fun updateCardField(cardId: Long, recalculate: Boolean = true, transform: (Card) -> Card) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId) ?: return@launch
            repository.updateCard(transform(card))
            if (recalculate) {
                repository.updateCardCalculations(cardId)
            }
        }
    }

    fun updateCardBagWeight(cardId: Long, bagWeight: Double) =
        updateCardField(cardId) { it.copy(bagWeight = bagWeight) }

    fun updateCardBagMethod(cardId: Long, isSampling: Boolean, sampleCount: Int, sampleTotalWeight: Double) =
        updateCardField(cardId) {
            it.copy(
                bagMethodIsSampling = isSampling,
                bagSampleCount = sampleCount,
                bagSampleTotalWeight = sampleTotalWeight
            )
        }

    fun updateCardImpurityWeight(cardId: Long, impurityWeight: Double) =
        updateCardField(cardId) { it.copy(impurityWeight = impurityWeight) }

    fun updateCardPricePerKg(cardId: Long, pricePerKg: Double) =
        updateCardField(cardId) { it.copy(pricePerKg = pricePerKg) }

    fun updateCardMoisture(cardId: Long, moisturePercent: Double) =
        updateCardField(cardId) { it.copy(moisturePercent = moisturePercent) }

    fun toggleCardLock(cardId: Long) =
        updateCardField(cardId, recalculate = false) { it.copy(isLocked = !it.isLocked) }

    override fun onCleared() {
        ttsManager.stop()
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
