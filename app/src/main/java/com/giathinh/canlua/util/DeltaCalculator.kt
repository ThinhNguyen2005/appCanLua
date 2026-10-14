package com.giathinh.canlua.util

fun calcDeltaPercent(current: Double, previous: Double?): Double? {
    if (previous == null || previous == 0.0) return null
    return (current - previous) / previous * 100.0
}