package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.location.LocationProvider
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.repository.CardRepository
import com.GiaThinh.canlua.repository.FirestoreRepository
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.util.RiceCalculator
import com.GiaThinh.canlua.util.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository: CardRepository,
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    val availableSeasons: StateFlow<List<String>> = repository.getDistinctSeasons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Số phiếu user đã tạo HÔM NAY (00:00 local timezone → bây giờ).
     * Reactive từ `getAllCards()` flow — tự động cập nhật khi user tạo/xoá phiếu.
     * UI dùng để check Premium quota (free user: 3 phiếu/ngày).
     */
    val cardsCreatedTodayCount: StateFlow<Int> = repository.getAllCards()
        .map { all ->
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            all.count { it.date.time >= startOfDay }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        ttsManager.initialize()
        ttsManager.setEnabled(settingsRepository.isTtsEnabled())
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
        seasonLabel: String = "",
        traderPhone: String = ""
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch

            // Lấy GPS + reverse geocoding (best-effort, không block tạo phiếu)
            val geo = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
            val address = geo?.let {
                runCatching { locationProvider.reverseGeocode(it.lat, it.lon) }.getOrNull()
            }.orEmpty()

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
                fieldAddress = address
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

    /**
     * Trader quét QR → lock card cả ở Firestore (để Sổ thương nhân thấy)
     * và ở Room local (để hiển thị trạng thái đồng bộ ngay).
     *
     * Nếu card chưa có ở local DB của trader (case thương lái khác máy với farmer),
     * vẫn cập nhật Firestore để Sổ thương nhân nhận diện qua observeMyTraderCards.
     */
    fun verifyAndLockTransaction(scannedToken: String, traderId: String) {
        viewModelScope.launch {
            // 1) Push lock state lên Firestore — bắt buộc, vì Sổ thương nhân query trực tiếp Firestore
            firestoreRepository.lockCardByQrToken(scannedToken, traderId)

            // 2) Update local Room nếu card có sẵn trong DB của thiết bị này
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

    fun updateCardTraderPhone(cardId: Long, phone: String) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(traderPhone = phone.trim()))
                loadCardById(cardId)
            }
        }
    }

    fun updateCardFieldAddress(cardId: Long, address: String) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId)
            card?.let {
                repository.updateCard(it.copy(fieldAddress = address.trim()))
                loadCardById(cardId)
            }
        }
    }

    /** Refresh GPS + địa chỉ ruộng cho phiếu hiện tại (gọi lại Geocoder) */
    fun refreshFieldLocation(cardId: Long) {
        viewModelScope.launch {
            val geo = runCatching { locationProvider.getCurrentLocation() }.getOrNull() ?: return@launch
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
