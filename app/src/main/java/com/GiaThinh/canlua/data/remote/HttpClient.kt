package com.GiaThinh.canlua.data.remote

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight HTTP client dùng OkHttp + Gson (đã có sẵn trong build.gradle).
 * Tránh thêm Retrofit để bundle không phình.
 */
@Singleton
class HttpClient @Inject constructor(
    @PublishedApi internal val client: OkHttpClient
) {
    @PublishedApi
    internal val gson = Gson()

    /** GET request → parse JSON về [T]. Throws nếu HTTP non-2xx hoặc parse fail. */
    inline fun <reified T> get(url: String, headers: Map<String, String> = emptyMap()): T {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.get().build()
        return executeAndParse(request)
    }

    /** POST JSON body → parse response. Hỗ trợ tự follow redirect cho Google Apps Script. */
    inline fun <reified T> postJson(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val json = gson.toJson(body)
        val reqBody = json.toRequestBody("application/json".toMediaType())
        
        // Tạo client tạm thời tắt tự động follow redirect để bắt mã 302 của GAS
        val noRedirectClient = client.newBuilder().followRedirects(false).build()
        
        var request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.post(reqBody).build()
        
        var response = noRedirectClient.newCall(request).execute()
        
        // GAS trả 302 redirect đến script.googleusercontent.com (CDN).
        // Phải POST lại nguyên body vì doPost handler cần payload.
        // Lưu ý: KHÔNG chuyển sang GET ở đây — sẽ mất body và doPost parse fail.
        if (response.code in listOf(301, 302, 303, 307, 308)) {
            val location = response.header("Location")
            response.close()
            if (location != null) {
                request = Request.Builder().url(location).apply {
                    headers.forEach { (k, v) -> addHeader(k, v) }
                }.post(reqBody).build()
                response = client.newCall(request).execute()
            }
        }
        
        response.use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw HttpException(resp.code, raw.take(500))
            }
            return try {
                gson.fromJson(raw, T::class.java)
            } catch (e: JsonSyntaxException) {
                throw HttpException(resp.code, "JSON parse error: ${e.message}")
            }
        }
    }

    @PublishedApi
    internal inline fun <reified T> executeAndParse(request: Request): T {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw HttpException(response.code, raw.take(500))
            }
            return try {
                gson.fromJson(raw, T::class.java)
            } catch (e: JsonSyntaxException) {
                throw HttpException(response.code, "JSON parse error: ${e.message}")
            }
        }
    }
}

class HttpException(val code: Int, val errorBody: String) :
    RuntimeException("HTTP $code: $errorBody")
