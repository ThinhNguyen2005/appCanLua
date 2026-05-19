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
class HttpClient @Inject constructor() {
    @PublishedApi
    internal val gson = Gson()

    @PublishedApi
    internal val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    /** GET request → parse JSON về [T]. Throws nếu HTTP non-2xx hoặc parse fail. */
    inline fun <reified T> get(url: String, headers: Map<String, String> = emptyMap()): T {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.get().build()
        return executeAndParse(request)
    }

    /** POST JSON body → parse response. */
    inline fun <reified T> postJson(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap()
    ): T {
        val json = gson.toJson(body)
        val reqBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.post(reqBody).build()
        return executeAndParse(request)
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
