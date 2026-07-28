package com.giathinh.canlua.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight Supabase REST API client using OkHttp + Gson.
 *
 * Supabase REST API: https://{ref}.supabase.co/rest/v1/{table}
 * Auth: apikey header (anon key for reads, service_role for writes).
 *
 * No extra dependencies — reuses the existing OkHttpClient singleton.
 */
@Singleton
class SupabaseClient @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "SupabaseClient"
    }

    /**
     * GET rows from a table with optional query params.
     * Returns parsed list — throws [SupabaseException] on HTTP error.
     */
    fun <T> getList(
        baseUrl: String,
        anonKey: String,
        table: String,
        params: Map<String, String> = emptyMap(),
        type: java.lang.reflect.Type
    ): List<T> {
        val urlBuilder = StringBuilder("$baseUrl/rest/v1/$table")
        if (params.isNotEmpty()) {
            urlBuilder.append("?")
            urlBuilder.append(params.entries.joinToString("&") { "${it.key}=${it.value}" })
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "GET $table failed ${response.code}: ${body.take(200)}")
                throw SupabaseException(response.code, body.take(200))
            }
            return try {
                gson.fromJson(body, type) ?: emptyList()
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "JSON parse error for $table: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Upsert (INSERT or UPDATE) a list of rows using Prefer: resolution=merge-duplicates.
     * Requires service_role key — should only be called from admin/GAS, not the app.
     */
    fun <T> upsert(
        baseUrl: String,
        serviceRoleKey: String,
        table: String,
        rows: List<T>
    ) {
        val json = gson.toJson(rows)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/rest/v1/$table")
            .header("apikey", serviceRoleKey)
            .header("Authorization", "Bearer $serviceRoleKey")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "UPSERT $table failed ${response.code}: ${responseBody.take(200)}")
                throw SupabaseException(response.code, responseBody.take(200))
            }
        }
    }
}

class SupabaseException(val code: Int, val errorBody: String) :
    RuntimeException("Supabase $code: $errorBody")
