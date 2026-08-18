package com.giathinh.canlua.di

import com.giathinh.canlua.core.AppInitializer
import com.giathinh.canlua.core.LiteAppInitializer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiteAppInitializerModule {

    @Binds
    @Singleton
    abstract fun bindAppInitializer(initializer: LiteAppInitializer): AppInitializer
}
