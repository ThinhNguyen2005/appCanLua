package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.dao.RicePriceDao
import com.giathinh.canlua.data.model.RicePrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LiteMarketViewModel @Inject constructor(
    ricePriceDao: RicePriceDao
) : ViewModel() {
    val prices: StateFlow<List<RicePrice>> = ricePriceDao.getAllPrices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
