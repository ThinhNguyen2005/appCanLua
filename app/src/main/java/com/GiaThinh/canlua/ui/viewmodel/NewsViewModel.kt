package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.NewsArticle
import com.GiaThinh.canlua.data.model.NewsTopic
import com.GiaThinh.canlua.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshAt: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val _selectedTopic = MutableStateFlow<NewsTopic?>(null)
    val selectedTopic: StateFlow<NewsTopic?> = _selectedTopic.asStateFlow()

    private val _ui = MutableStateFlow(NewsUiState())
    val ui: StateFlow<NewsUiState> = _ui.asStateFlow()

    /** List bài lọc theo selectedTopic, hot stream cache 5s sau khi không còn subscriber. */
    val articles: StateFlow<List<NewsArticle>> = _selectedTopic
        .flatMapLatest { topic -> repository.observe(topic, limit = 30) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        // Auto refresh khi mở app: empty cache hoặc cache > 1 giờ
        viewModelScope.launch {
            if (repository.isEmpty() || repository.isStale()) {
                refresh()
            }
        }
    }

    fun selectTopic(topic: NewsTopic?) {
        _selectedTopic.value = topic
    }

    fun refresh() {
        if (_ui.value.isRefreshing) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isRefreshing = true, errorMessage = null)
            val result = repository.refresh()
            _ui.value = _ui.value.copy(
                isRefreshing = false,
                errorMessage = result.exceptionOrNull()?.message?.takeIf { result.isFailure },
                lastRefreshAt = if (result.isSuccess) System.currentTimeMillis() else _ui.value.lastRefreshAt
            )
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(errorMessage = null)
    }
}
