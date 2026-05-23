package com.GiaThinh.canlua.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared state holder để các tab screen (CardListScreen, ...) báo MainScreen
 * ẩn/hiện ModernBottomBar theo hướng cuộn. Tránh che FAB và nội dung dài.
 *
 * Quy ước: mặc định hiện. Khi rời tab nên reset về true để các tab khác không
 * kế thừa trạng thái ẩn.
 */
object BottomBarVisibility {
    private val _visible = MutableStateFlow(true)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun set(value: Boolean) {
        if (_visible.value != value) _visible.value = value
    }

    fun reset() {
        _visible.value = true
    }
}
