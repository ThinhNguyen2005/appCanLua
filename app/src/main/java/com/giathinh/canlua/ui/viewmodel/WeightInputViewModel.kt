package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.BuildConfig
import com.giathinh.canlua.R
import com.giathinh.canlua.data.bluetooth.MockScaleClient
import com.giathinh.canlua.data.bluetooth.PairedScaleDevice
import com.giathinh.canlua.data.bluetooth.ScaleCaptureMode
import com.giathinh.canlua.data.bluetooth.ScaleConnectionException
import com.giathinh.canlua.data.bluetooth.ScaleConnectionState
import com.giathinh.canlua.data.bluetooth.ScaleDataSource
import com.giathinh.canlua.data.bluetooth.ScaleReading
import com.giathinh.canlua.data.bluetooth.ScaleReadingStabilizer
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.domain.WeighValidation
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.SyncableCardRepository
import com.giathinh.canlua.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeightInputViewModel @Inject constructor(
    private val repository: SyncableCardRepository,
    private val ttsManager: TextToSpeechManager,
    private val settingsRepository: SettingsRepository,
    private val scaleDataSource: ScaleDataSource,
    private val mockScaleClient: MockScaleClient,
    private val scaleStabilizer: ScaleReadingStabilizer
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

    private val _scaleCaptureEvents = MutableSharedFlow<Double>(extraBufferCapacity = 8)
    val scaleCaptureEvents = _scaleCaptureEvents.asSharedFlow()

    private var scaleConnectionJob: Job? = null

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

    /**
     * T02/T08: Preview tính từ TRỌNG LƯỢNG THẬT từng bao qua [CardSettlement],
     * chạy trên Default dispatcher (không chặn main thread). UI đọc totalNetKg và
     * totalAmountVnd từ cùng kết quả — khớp tuyệt đối với quyết toán trong DB.
     */
    val settlement: StateFlow<com.giathinh.canlua.util.RiceSettlement.Settlement?> =
        combine(currentCard, weightEntries) { card, entries ->
            if (card == null) null
            else withContext(Dispatchers.Default) {
                com.giathinh.canlua.domain.CardSettlement.preview(card, entries)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        const val WEIGHT_ROUNDING_FACTOR = 100.0
        const val MOCK_DEVICE_ADDRESS = "00:11:22:33:44:55"
    }

    fun addWeightEntryDirectly(cardId: Long, weight: Double) {
        if (!weight.isFinite() || weight <= 0.0 || weight > MAX_BAG_KG) return
        viewModelScope.launch {
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

        viewModelScope.launch {
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
        viewModelScope.launch {
            // patchCard đọc–kiểm–ghi trong 1 transaction (chống lost-update), tự recalc.
            repository.patchCard(cardId, recalculate = recalculate, transform = transform)
        }
    }

    fun updateCardBagWeight(cardId: Long, bagWeight: Double) =
        updateCardField(cardId) { it.copy(bagWeight = bagWeight) }

    fun updateCardBagMethod(cardId: Long, isSampling: Boolean, sampleCount: Int, sampleTotalWeight: Double) =
        updateCardField(cardId) {
            val rule = com.giathinh.canlua.domain.WeighDefaultsResolver.normalizeBagRule(
                isSampling = isSampling,
                sampleCount = sampleCount,
                sampleTotalWeight = sampleTotalWeight,
                fallbackBagWeight = it.bagWeight
            )
            it.copy(
                bagWeight = rule.bagWeight,
                bagMethodIsSampling = rule.isSampling,
                bagSampleCount = rule.sampleCount,
                bagSampleTotalWeight = rule.sampleTotalWeight
            )
        }

    fun updateCardImpurityWeight(cardId: Long, impurityWeight: Double) =
        updateCardField(cardId) { it.copy(impurityWeight = impurityWeight) }

    fun updateCardPricePerKg(cardId: Long, pricePerKg: Double) =
        updateCardField(cardId) { it.copy(pricePerKg = pricePerKg) }

    fun updateCardMoisture(cardId: Long, moisturePercent: Double) =
        updateCardField(cardId) { it.copy(moisturePercent = moisturePercent) }

    fun toggleCardLock(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId) ?: return@launch
            repository.setCardLocked(cardId, card.revision, !card.isLocked)
        }
    }

    fun refreshScaleStatus() {
        _weightInputState.update { state ->
            state.copy(
                hasBluetoothPermission = scaleDataSource.hasRequiredPermissions(),
                isBluetoothEnabled = scaleDataSource.isBluetoothEnabled(),
                pairedScaleDevices = scaleDataSource.getPairedDevices()
            )
        }
    }

    fun setScaleCaptureMode(mode: ScaleCaptureMode) {
        scaleStabilizer.clearPendingWindow()
        _weightInputState.update { state ->
            val updatedConn = (state.scaleConnectionState as? ScaleConnectionState.Connected)?.let {
                if (mode == ScaleCaptureMode.SEMI_AUTO) it.copy(isWaitingForZero = false) else it
            } ?: state.scaleConnectionState
            state.copy(scaleCaptureMode = mode, scaleConnectionState = updatedConn)
        }
    }

    fun connectToScale(device: PairedScaleDevice) {
        if (device.isMock && !BuildConfig.DEBUG) return
        disconnectScaleInternal(resetToDisconnected = false)
        scaleStabilizer.reset()
        _weightInputState.update {
            it.copy(scaleConnectionState = ScaleConnectionState.Connecting(device.name))
        }
        scaleConnectionJob = viewModelScope.launch {
            val stream = if (device.isMock) {
                mockScaleClient.observeSimulatedReadings()
            } else {
                scaleDataSource.connect(device)
            }
            stream
                .catch { error ->
                    if (error is CancellationException) throw error
                    val msgRes = (error as? ScaleConnectionException)?.messageResId
                        ?: R.string.scale_error_connection_lost
                    _weightInputState.update {
                        it.copy(scaleConnectionState = ScaleConnectionState.Error(msgRes))
                    }
                }
                .collect { reading -> handleScaleReading(device, reading) }
        }
    }

    fun connectMockScale(displayName: String) {
        if (!BuildConfig.DEBUG) return
        connectToScale(PairedScaleDevice(name = displayName, address = MOCK_DEVICE_ADDRESS, isMock = true))
    }

    fun disconnectScale() {
        disconnectScaleInternal(resetToDisconnected = true)
    }

    fun captureCurrentScaleWeight() {
        val connected = _weightInputState.value.scaleConnectionState as? ScaleConnectionState.Connected ?: return
        if (currentCard.value?.isLocked == true) return
        if (!WeighValidation.isValidWeight(connected.liveWeightKg)) return
        scaleStabilizer.clearPendingWindow()
        recordScaleBag(connected.liveWeightKg)
    }

    private fun handleScaleReading(device: PairedScaleDevice, reading: ScaleReading) {
        val mode = _weightInputState.value.scaleCaptureMode
        val output = scaleStabilizer.onReading(reading, mode)
        _weightInputState.update { state ->
            state.copy(
                scaleConnectionState = ScaleConnectionState.Connected(
                    deviceName = device.name,
                    liveWeightKg = output.liveWeightKg,
                    isStable = output.isStable,
                    isWaitingForZero = output.isWaitingForZero,
                    isMock = device.isMock
                )
            )
        }
        val captured = output.capturedWeightKg
        if (captured != null && currentCard.value?.isLocked != true) {
            recordScaleBag(captured)
        }
    }

    private fun recordScaleBag(rawWeightKg: Double) {
        val rounded = (rawWeightKg * WEIGHT_ROUNDING_FACTOR).roundToInt() / WEIGHT_ROUNDING_FACTOR
        val activeCardId = _cardId.value ?: return
        if (!WeighValidation.isValidWeight(rounded)) return
        addWeightEntryDirectly(activeCardId, rounded)
        _scaleCaptureEvents.tryEmit(rounded)
    }

    private fun disconnectScaleInternal(resetToDisconnected: Boolean) {
        scaleConnectionJob?.cancel()
        scaleConnectionJob = null
        scaleDataSource.disconnect()
        scaleStabilizer.reset()
        if (resetToDisconnected) {
            _weightInputState.update {
                it.copy(scaleConnectionState = ScaleConnectionState.Disconnected)
            }
        }
    }

    override fun onCleared() {
        disconnectScaleInternal(resetToDisconnected = false)
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

internal fun organizeIntoTables(entries: List<WeightEntry>, manualCount: Int): List<List<List<Double?>>> {
    val totalEntries = entries.size
    val calculatedNumTables = ((totalEntries - 1).coerceAtLeast(0) / 25) + 1
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
