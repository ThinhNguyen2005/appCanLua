package com.GiaThinh.canlua.util

import com.GiaThinh.canlua.BuildConfig
import android.util.Log
import android.util.SparseIntArray
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.FrameMetricsAggregator
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * PerformanceTracker - Bộ công cụ quản lý hiệu năng ứng dụng tập trung.
 * Sử dụng Firebase Performance Monitoring SDK và Androidx FrameMetricsAggregator
 * để tự động đo lường thời gian tải màn hình, CPU/RAM, và các vấn đề về rớt khung hình (Slow/Frozen frames).
 */
object PerformanceTracker {
    private const val TAG = "PerformanceTracker"
    
    // Yếu tố tham chiếu tới Activity hiện tại để thu thập FrameMetrics
    private var activityRef = WeakReference<ComponentActivity>(null)
    
    // Lưu trữ các Trace đang chạy của màn hình (đo lường thời gian sử dụng, CPU, RAM)
    private val activeScreenTraces = java.util.concurrent.ConcurrentHashMap<String, Trace>()
    
    // Lưu trữ các Trace đo lường thời gian tải trang (Time to First Render)
    private val activeLoadTraces = java.util.concurrent.ConcurrentHashMap<String, Trace>()
    
    // Bộ thu thập chỉ số vẽ khung hình từ Android OS
    private var frameMetricsAggregator: FrameMetricsAggregator? = null
    
    // Tên màn hình đang được active trace
    private var currentActiveRoute: String? = null

    /**
     * Dữ liệu thống kê khung hình vẽ giao diện.
     */
    data class FrameMetricsData(
        val totalFrames: Int,
        val slowFrames: Int,
        val frozenFrames: Int
    )

    /**
     * Liên kết Activity hiện tại vào Tracker khi Activity được khởi tạo.
     */
    fun setActivity(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
        Log.d(TAG, "Activity registered: ${activity.javaClass.simpleName}")
    }

    /**
     * Hủy liên kết Activity khi bị destroy để tránh memory leak.
     */
    fun clearActivity() {
        stopFrameTracking()
        activityRef.clear()
        Log.d(TAG, "Activity cleared")
    }

    /**
     * Xử lý chuyển đổi màn hình:
     * - Kết thúc đo đạc màn hình cũ (bao gồm tính toán rớt khung hình).
     * - Khởi động đo đạc màn hình mới.
     */
    fun onScreenChanged(route: String?) {
        if (route.isNullOrBlank()) return
        
        val sanitizedRoute = sanitizeRouteName(route)
        if (currentActiveRoute == sanitizedRoute) return
        
        Log.d(TAG, "Screen transition: $currentActiveRoute -> $sanitizedRoute")
        
        // 1. Dừng trace màn hình cũ
        currentActiveRoute?.let { oldRoute ->
            stopScreenTrace(oldRoute)
        }
        
        // 2. Cập nhật route hiện tại và khởi chạy trace mới
        currentActiveRoute = sanitizedRoute
        startScreenTrace(sanitizedRoute)
        
        // 3. Khởi động trace đo thời gian tải trang (First composition)
        startLoadTrace(sanitizedRoute)
    }

    /**
     * Bắt đầu một Custom Trace để theo dõi thời gian lưu lại, CPU và RAM của màn hình.
     */
    private fun startScreenTrace(routeName: String) {
        try {
            val traceKey = "screen_$routeName"
            Log.d(TAG, "Starting screen trace: $traceKey")
            
            val trace = FirebasePerformance.getInstance().newTrace(traceKey)
            trace.start()
            activeScreenTraces[routeName] = trace
            
            // Bắt đầu thu thập chỉ số khung hình của Activity hiện tại
            val activity = activityRef.get()
            if (activity != null) {
                startFrameTracking(activity)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error starting screen trace: ${e.message}")
        }
    }

    /**
     * Kết thúc Custom Trace của màn hình và đính kèm chỉ số rớt khung hình (slow/frozen frames).
     *
     * PERF FIX: stopFrameTracking() gọi aggregator.stop() — API này phải chạy trên
     * Main Thread nhưng có thể mất 5-15ms nếu SparseIntArray lớn. Để giảm thiểu
     * Main Thread contention, ta giữ lệnh stop() nhanh gọn (chỉ extract data) và
     * đẩy toàn bộ tính toán + trace.stop() sang background.
     */
    private fun stopScreenTrace(routeName: String) {
        try {
            val trace = activeScreenTraces.remove(routeName) ?: return
            val traceKey = "screen_$routeName"
            Log.d(TAG, "Stopping screen trace: $traceKey")
            
            // Lấy dữ liệu rớt khung hình nhanh trên Main Thread (chỉ extract data)
            val totalDurationArray = stopFrameTracking()
            
            // Đẩy toàn bộ tính toán mảng và Firebase Trace dừng sang Background Thread
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    if (totalDurationArray != null) {
                        var totalFrames = 0
                        var slowFrames = 0
                        var frozenFrames = 0
                        
                        for (i in 0 until totalDurationArray.size()) {
                            val durationMs = totalDurationArray.keyAt(i)
                            val frameCount = totalDurationArray.valueAt(i)
                            totalFrames += frameCount
                            
                            if (durationMs > 16) slowFrames += frameCount
                            if (durationMs > 700) frozenFrames += frameCount
                        }
                        
                        trace.putMetric("total_frames", totalFrames.toLong())
                        trace.putMetric("slow_frames", slowFrames.toLong())
                        trace.putMetric("frozen_frames", frozenFrames.toLong())
                        
                        if (totalFrames > 0) {
                            val slowPercent = slowFrames.toFloat() / totalFrames * 100
                            Log.d(TAG, "[$traceKey] Total: $totalFrames | Slow: $slowFrames (${String.format(java.util.Locale.US, "%.1f", slowPercent)}%) | Frozen: $frozenFrames")
                        }
                    }
                    trace.stop()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error processing frame metrics in background: ${e.message}")
                    // Ensure trace is always stopped to prevent resource leak
                    try { trace.stop() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error stopping screen trace: ${e.message}")
        }
    }

    /**
     * Khởi động đo đạc thời gian tải màn hình (Time to First Render).
     */
    private fun startLoadTrace(routeName: String) {
        try {
            val traceKey = "load_$routeName"
            Log.d(TAG, "Starting load trace: $traceKey")
            val trace = FirebasePerformance.getInstance().newTrace(traceKey)
            trace.start()
            activeLoadTraces[routeName] = trace
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error starting load trace: ${e.message}")
        }
    }

    /**
     * Kết thúc đo đạc tải màn hình (được gọi từ LaunchedEffect của Composable).
     */
    fun stopLoadTrace(route: String?) {
        if (route.isNullOrBlank()) return
        val sanitizedRoute = sanitizeRouteName(route)
        try {
            val trace = activeLoadTraces.remove(sanitizedRoute) ?: return
            val traceKey = "load_$sanitizedRoute"
            Log.d(TAG, "Stopping load trace (First render success): $traceKey")
            trace.stop()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error stopping load trace for $sanitizedRoute: ${e.message}")
        }
    }

    /**
     * Khởi động theo dõi các chỉ số khung hình của Window sử dụng FrameMetricsAggregator.
     */
    private fun startFrameTracking(activity: ComponentActivity) {
        try {
            if (frameMetricsAggregator == null) {
                frameMetricsAggregator = FrameMetricsAggregator(FrameMetricsAggregator.TOTAL_DURATION)
            }
            frameMetricsAggregator?.add(activity)
            Log.d(TAG, "Frame tracking started on window")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to start frame tracking: ${e.message}")
        }
    }

    /**
     * Kết thúc thu thập chỉ số khung hình và trả về mảng thời gian thô.
     * SAFETY: Luôn reset frameMetricsAggregator = null dù có exception hay không,
     * tránh stuck state khi aggregator.stop() throw.
     */
    private fun stopFrameTracking(): SparseIntArray? {
        val aggregator = frameMetricsAggregator ?: return null
        frameMetricsAggregator = null // Reset TRƯỚC khi gọi stop() để tránh stuck state
        return try {
            val metrics = aggregator.stop()
            if (metrics != null && metrics.isNotEmpty()) {
                metrics[FrameMetricsAggregator.TOTAL_INDEX]
            } else null
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error stopping frame metrics: ${e.message}")
            null
        }
    }

    /**
     * Chuẩn hóa tên route để hợp lệ với quy tắc đặt tên trace của Firebase Performance
     * (Không chứa kí tự đặc biệt, không quá 100 kí tự, không bắt đầu bằng '_').
     */
    private fun sanitizeRouteName(route: String): String {
        var clean = route.replace(Regex("[^a-zA-Z0-9]"), "_")
        // Rút gọn bớt độ dài của tham số động (ví dụ: weight_input__cardId_ -> weight_input)
        if (clean.contains("_cardId_")) {
            clean = clean.substringBefore("_cardId_") + "_detail"
        }
        clean = clean.trim('_').take(80)
        return if (clean.isEmpty()) "unknown" else clean
    }
}

/**
 * Composable Helper tự động báo hiệu khi màn hình vẽ xong lần đầu tiên.
 * Chỉ cần gọi `TrackScreenRender("tên_màn_hình")` ở đầu Composable chính.
 */
@Composable
fun TrackScreenRender(screenRoute: String) {
    LaunchedEffect(screenRoute) {
        PerformanceTracker.stopLoadTrace(screenRoute)
    }
}
