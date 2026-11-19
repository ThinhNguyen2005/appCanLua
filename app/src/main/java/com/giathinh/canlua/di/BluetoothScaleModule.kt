package com.giathinh.canlua.di

import com.giathinh.canlua.data.bluetooth.BluetoothClassicScaleClient
import com.giathinh.canlua.data.bluetooth.CompositeScaleParser
import com.giathinh.canlua.data.bluetooth.ScaleClock
import com.giathinh.canlua.data.bluetooth.ScaleDataSource
import com.giathinh.canlua.data.bluetooth.ScaleProtocolParser
import com.giathinh.canlua.data.bluetooth.ScaleReadingStabilizer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothScaleModule {

    @Binds
    @Singleton
    abstract fun bindScaleDataSource(impl: BluetoothClassicScaleClient): ScaleDataSource

    companion object {
        @Provides
        @Singleton
        fun provideScaleProtocolParser(): ScaleProtocolParser = CompositeScaleParser()

        @Provides
        @Singleton
        fun provideScaleClock(): ScaleClock = ScaleClock.SYSTEM

        @Provides
        fun provideScaleReadingStabilizer(clock: ScaleClock): ScaleReadingStabilizer =
            ScaleReadingStabilizer(clock)
    }
}
