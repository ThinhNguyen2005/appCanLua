package com.giathinh.canlua.di

import com.giathinh.canlua.ads.AdConsentManager
import com.giathinh.canlua.ads.ConsentProvider
import com.giathinh.canlua.ads.NativeAdLoaderBridge
import com.giathinh.canlua.ads.SdkNativeAdLoaderBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings cho lớp ads/consent:
 *  - [AdConsentManager] (implementation của [ConsentProvider]) — injectable thay cho object singleton.
 *  - [SdkNativeAdLoaderBridge] — bridge mặc định gọi NativeAdLoader tĩnh của Next-Gen SDK;
 *    unit test thay bằng fake để drive logic production.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindConsentManager(impl: AdConsentManager): ConsentProvider

    @Binds
    @Singleton
    abstract fun bindNativeAdLoaderBridge(impl: SdkNativeAdLoaderBridge): NativeAdLoaderBridge
}
