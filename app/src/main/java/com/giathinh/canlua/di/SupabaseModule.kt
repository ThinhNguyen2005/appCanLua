package com.giathinh.canlua.di

import com.giathinh.canlua.BuildConfig
import com.giathinh.canlua.data.remote.SupabaseClient
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    /** Supabase project base URL — from local.properties → BuildConfig. */
    @Provides
    @Named("supabaseUrl")
    fun provideSupabaseUrl(): String =
        BuildConfig.SUPABASE_URL

    /** Supabase anon key — safe to bundle, only allows public RLS reads. */
    @Provides
    @Named("supabaseAnonKey")
    fun provideSupabaseAnonKey(): String =
        BuildConfig.SUPABASE_ANON_KEY

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideSupabaseClient(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): SupabaseClient = SupabaseClient(okHttpClient, gson)
}
