package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.giathinh.canlua.data.location.LocationProvider
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.di.IoDispatcher
import com.giathinh.canlua.di.MainDispatcher
import java.util.Date

@HiltViewModel
class CardListViewModel @Inject constructor(
    private val repository: CardRepository,
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    @param:IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: kotlinx.coroutines.CoroutineDispatcher
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteEvents = MutableSharedFlow<DeleteCardEvent>()
    val deleteEvents = _deleteEvents.asSharedFlow()

    private val _selectedVarietyFilter = MutableStateFlow<String?>(null)
    val selectedVarietyFilter: StateFlow<String?> = _selectedVarietyFilter.asStateFlow()

    private val _selectedSeasonFilter = MutableStateFlow<String?>(null)
    val selectedSeasonFilter: StateFlow<String?> = _selectedSeasonFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPaymentFilter = MutableStateFlow<Boolean?>(null) // null = all, true = paid, false = unpaid
    val selectedPaymentFilter: StateFlow<Boolean?> = _selectedPaymentFilter.asStateFlow()

    val cards: StateFlow<List<Card>> = combine(
        repository.getAllCards(),
        _selectedVarietyFilter,
        _selectedSeasonFilter,
        _searchQuery,
        _selectedPaymentFilter
    ) { all, variety, season, query, payment ->
        all.filter { card ->
            (variety == null || card.riceVariety.equals(variety, ignoreCase = true)) &&
            (season == null || card.seasonLabel.equals(season, ignoreCase = true)) &&
            (query.isBlank() || card.traderName.contains(query, ignoreCase = true) || card.riceVariety.contains(query, ignoreCase = true)) &&
            (payment == null || card.isPaid == payment)
        }
    }.onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableVarieties: StateFlow<List<String>> = repository.getDistinctRiceVarieties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggestedRiceVarieties: StateFlow<List<String>> = repository.getSuggestedRiceVarieties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableSeasons: StateFlow<List<String>> = repository.getDistinctSeasons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshCards() {
        _isLoading.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPaymentFilter(payment: Boolean?) {
        _selectedPaymentFilter.value = payment
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
        _searchQuery.value = ""
        _selectedPaymentFilter.value = null
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
        impurityWeight: Double = 0.0,
        recordLocation: Boolean = false,
        onCreated: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch

            val geo = if (recordLocation) {
                runCatching { locationProvider.getCurrentLocation(forceFresh = true) }.getOrNull()
            } else null
            val address = geo?.let {
                runCatching { locationProvider.reverseGeocode(it.lat, it.lon) }.getOrNull()
            }.orEmpty()

            val defaults = settingsRepository.getWeighDefaults()
            val computedBagWeight = if (defaults.bagMethodIsSampling && defaults.bagSampleCount > 0) {
                defaults.bagSampleTotalWeight / defaults.bagSampleCount
            } else if (defaults.bagSampleCount > 0) {
                1.0 / defaults.bagSampleCount
            } else {
                1.0 / 8.0
            }

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
                bagWeight = computedBagWeight,
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
                    com.giathinh.canlua.data.model.Transaction(
                        cardId = cardId,
                        amount = depositAmount,
                        type = com.giathinh.canlua.data.model.TransactionType.DEPOSIT,
                        description = "Tiền cọc"
                    )
                )
                repository.updateCardCalculations(cardId)
            }

            if (onCreated != null) {
                kotlinx.coroutines.withContext(mainDispatcher) {
                    onCreated(cardId)
                }
            }
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch(ioDispatcher) {
            val event = runCatching {
                repository.deleteCard(card)
            }.fold(
                onSuccess = { DeleteCardEvent.Success(card.name) },
                onFailure = { DeleteCardEvent.Error }
            )
            _deleteEvents.emit(event)
        }
    }
}

sealed interface DeleteCardEvent {
    data class Success(val cardName: String) : DeleteCardEvent
    data object Error : DeleteCardEvent
}
