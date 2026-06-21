package com.giathinh.canlua.di

import com.giathinh.canlua.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            // M-17: Tích hợp Retry Interceptor
            .addInterceptor { chain ->
                val request = chain.request()
                var response: okhttp3.Response? = null
                var exception: java.io.IOException? = null
                var tryCount = 0
                val maxLimit = 3
                
                while (tryCount < maxLimit) {
                    tryCount++
                    try {
                        response = chain.proceed(request)
                        if (response.isSuccessful) {
                            return@addInterceptor response
                        }
                        // Không retry đối với các lỗi client (4xx) trừ Request Timeout (408)
                        if (response.code in 400..499 && response.code != 408) {
                            return@addInterceptor response
                        }
                        if (tryCount < maxLimit) {
                            response.close()
                            Thread.sleep(1000L * tryCount)
                        }
                    } catch (e: java.io.IOException) {
                        exception = e
                        if (tryCount < maxLimit) {
                            Thread.sleep(1000L * tryCount)
                        }
                    }
                }
                if (response != null) {
                    response
                } else {
                    throw exception ?: java.io.IOException("Yêu cầu mạng thất bại sau $maxLimit lần thử")
                }
            }
            .build()
    }
}
