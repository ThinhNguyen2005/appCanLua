package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.location.LocationProvider
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.repository.FirestoreRepository
import com.GiaThinh.canlua.repository.SyncableCardRepository
import com.GiaThinh.canlua.repository.QrLockResult
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.util.RiceCalculator
import com.GiaThinh.canlua.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

sealed class QrVerificationState {
    data object Idle : QrVerificationState()
    data object Loading : QrVerificationState()
    data class Success(val firestoreId: String) : QrVerificationState()
    data class AlreadyConfirmed(val firestoreId: String) : QrVerificationState()
    data object NotFound : QrVerificationState()
    data object LockedByOtherTrader : QrVerificationState()
    data class Error(val message: String?) : QrVerificationState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository: SyncableCardRepository,
    private val ttsManager: TextToSpeechManager,
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val firestoreRepository: FirestoreRepository
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

    private val _qrVerificationState = MutableStateFlow<QrVerificationState>(QrVerificationState.Idle)
    val qrVerificationState: StateFlow<QrVerificationState> = _qrVerificationState.asStateFlow()

    // TTS enabled — observe StateFlow để tránh poll SharedPreferences mỗi keystroke
    private val ttsEnabledState: StateFlow<Boolean> = settingsRepository.ttsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.isTtsEnabled())

    // === Filter state — cả 2 filter combine với nhau ===
    private val _selectedVarietyFilter = MutableStateFlow<String?>(null)
    val selectedVarietyFilter: StateFlow<String?> = _selectedVarietyFilter.asStateFlow()

    private val _selectedSeasonFilter = MutableStateFlow<String?>(null)
    val selectedSeasonFilter: StateFlow<String?> = _selectedSeasonFilter.asStateFlow()

    /**
     * Cards reactive: combine 2 filters → flatMapLatest lấy từ DB.
     * Vì Room DAO chỉ có query single-filter, ở đây lấy allCards rồi filter tại memory —
     * với data nhỏ (vai trăm phiếu/vụ) đây là chi phí không đáng kể và đơn giản hơn viết DAO mới.
     */
    val cards: StateFlow<List<Card>> = combine(
        repository.getAllCards(),
        _selectedVarietyFilter,
        _selectedSeasonFilter
    ) { all, variety, season ->
        all.filter { card ->
            (variety == null || card.riceVariety == variety) &&
                    (season == null || card.seasonLabel == season)
        }
    }.onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableVarieties: StateFlow<List<String>> = repository.getDistinctRiceVarieties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggestedRiceVarieties: StateFlow<List<String>> = repository.getSuggestedRiceVarieties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableSeasons: StateFlow<List<String>> = repository.getDistinctSeasons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())



    init {
        // Khởi tạo CardViewModel - không tự động kích hoạt TTS tại đây để tối ưu hóa hiệu năng chuyển màn hình
    }

    fun refreshCards() {
        // Cards đã tự reactive qua Room Flow — chỉ ping isLoading để UI hiển thị pull-to-refresh.
        _isLoading.value = false
    }

    fun setVarietyFilter(variety: String?) {
        _selectedVarietyFilter.value = variety
    }

    fun setSeasonFilter(season: String?) {
        _selectedSeasonFilter.value = season
    }

    fun clearFilters() {
        _selectedVarietyFilter.value = null
        _selectedSeasonFilter.value = null
    }

    fun loadCardById(cardId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _manualTableCount.value = 0

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

    fun incrementManualTableCount() {
        _manualTableCount.value = _manualTableCount.value + 1
    }

    /**
     * Chỉ kích hoạt và khởi tạo Text-To-Speech khi người dùng vào màn hình nhập cân (WeightInputScreen).
     * Tránh khởi tạo ở các màn hình khác (danh sách, chi tiết, bản đồ...) gây nghẽn luồng chính (Main Thread)
     * do giao dịch binder đồng bộ với dịch vụ TTS của hệ thống.
     */
    fun startTts() {
        viewModelScope.launch(Dispatchers.IO) {
            ttsManager.setEnabled(ttsEnabledState.value)
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
        seasonLabel: String = "",
        traderPhone: String = "",
        bagWeight: Double = 0.0,
        impurityWeight: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch

            // Lấy GPS + reverse geocoding (best-effort, không block tạo phiếu)
            // forceFresh=true vì phiếu mới cần geo chính xác lúc cân (cache 6h có thể stale).
            val geo = runCatching { locationProvider.getCurrentLocation(forceFresh = true) }.getOrNull()
            val address = geo?.let {
                runCatching { locationProvider.reverseGeocode(it.lat, it.lon) }.getOrNull()
            }.orEmpty()

            val defaults = settingsRepository.getWeighDefaults()
            val newCard = Card(
                name = trimmedName,
                cccd = cccd,
                traderName = traderName.trim(),
                date = Date(),
                pricePerKg = pricePerKg,
                depositAmount = depositAmount,
                riceVariety = riceVariety,
                moisturePercent = moisturePercent,
                seasonLabel = seasonLabel,
                latitude = geo?.lat,
                longitude = geo?.lon,
                traderPhone = traderPhone.trim(),
                fieldAddress = address,
                bagWeight = bagWeight,
                impurityWeight = impurityWeight,
                impurityIsPercent = defaults.impurityIsPercent,
                bagMethodIsSampling = defaults.bagMethodIsSampling,
                bagSampleCount = defaults.bagSampleCount,
                bagSampleTotalWeight = defaults.bagSampleTotalWeight,
                weightInputMode = defaults.weightInputMode
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
                // Resync card cache (deposit field, totalAmount, remainingAmount) sau khi insert
                // để đảm bảo Card.depositAmount = sum(Transaction).
                repository.updateCardCalculations(cardId)
            }
        }
    }

    /**
     * Update card metadata + thay đổi tiền cọc / đã trả.
     *
     * **Source of truth:** `Transaction` table.
     * - `Card.depositAmount` và `Card.paidAmount` chỉ là CACHE của sum(Transaction).
     * - Mọi thay đổi deposit/paid PHẢI đi qua [Transaction] (delta entries),
     *   rồi [updateCardCalculations] resync lại field cache.
     * - Tuyệt đối không sửa trực tiếp `Card.depositAmount`/`paidAmount` ngoài fn này
     *   và [createCard], nếu không sẽ bị [updateCardCalculations] ghi đè.
     */
    fun updateCard(card: Card) {
        val current = _currentCard.value
        if (current != null && current.id == card.id) {
            _currentCard.value = card
        }
        viewModelScope.launch(Dispatchers.IO) {
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
            } else {
                val latestCard = repository.getCardById(card.id)
                if (latestCard != null) {
                    _currentCard.value = latestCard
                }
            }
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCard(card)
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

    fun addPayment(cardId: Long, amount: Double, description: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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

    /**
     * Trader quét QR → lock card cả ở Firestore (để Sổ thương nhân thấy)
     * và ở Room local (để hiển thị trạng thái đồng bộ ngay).
     *
     * Nếu card chưa có ở local DB của trader (case thương lái khác máy với farmer),
     * vẫn cập nhật Firestore để Sổ thương nhân nhận diện qua observeMyTraderCards.
     */
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
            _currentCard.value = repository.getCardById(card.id)
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

    fun updateCardName(cardId: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isBlank()) return@launch
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(name = name.trim()))
            }
        }
    }

    fun updateCardBagWeight(cardId: Long, bagWeight: Double) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(riceVariety = variety))
                loadCardById(cardId)
            }
        }
    }

    fun updateCardSeasonLabel(cardId: Long, seasonLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(seasonLabel = seasonLabel))
                loadCardById(cardId)
            }
        }
    }

    fun updateCardTraderPhone(cardId: Long, phone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(traderPhone = phone.trim()))
                loadCardById(cardId)
            }
        }
    }

    fun updateCardFieldAddress(cardId: Long, address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(fieldAddress = address.trim()))
                loadCardById(cardId)
            }
        }
    }

    /** Refresh GPS + địa chỉ ruộng cho phiếu hiện tại (gọi lại Geocoder) */
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
            loadCardById(cardId)
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

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    /**
     * Cập nhật 5 mode flag của phiếu (kg/% tạp, A/B bao, SMALL/LARGE quy cách KG).
     * Gộp 1 hàm thay vì 5 hàm riêng để chỉ chạy 1 lần update + 1 lần recalc khi
     * user đóng sheet "Tùy chọn cân".
     */
    fun updateCardWeighOptions(
        cardId: Long,
        impurityIsPercent: Boolean,
        bagMethodIsSampling: Boolean,
        bagSampleCount: Int,
        bagSampleTotalWeight: Double,
        weightInputMode: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
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
