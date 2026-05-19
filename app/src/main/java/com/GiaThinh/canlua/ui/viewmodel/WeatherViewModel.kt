package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.repository.WeatherRepository
import com.GiaThinh.canlua.repository.WeatherState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherUiState(
    val weather: WeatherInfo? = null,
    val isLoading: Boolean = false,
    val isStale: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private var observerJob: Job? = null

    init {
        observe(forceRefresh = false)
    }

    private fun observe(forceRefresh: Boolean) {
        observerJob?.cancel()
        observerJob = viewModelScope.launch {
            weatherRepository.observeWeather(forceRefresh = forceRefresh).collect { st ->
                _state.value = when (st) {
                    is WeatherState.Loading -> _state.value.copy(
                        isLoading = true,
                        errorMessage = null
                    )
                    is WeatherState.Data -> WeatherUiState(
                        weather = st.info,
                        isLoading = false,
                        isStale = st.isStale,
                        errorMessage = null
                    )
                    is WeatherState.Error -> _state.value.copy(
                        isLoading = false,
                        errorMessage = st.message
                    )
                }
            }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        observe(forceRefresh = forceRefresh)
    }

    fun onPermissionGranted() {
        load(forceRefresh = true)
    }
}
