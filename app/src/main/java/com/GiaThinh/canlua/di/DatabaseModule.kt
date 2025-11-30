package com.GiaThinh.canlua.di

import android.content.Context
import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
import com.GiaThinh.canlua.data.database.AppDatabase
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.util.TextToSpeechManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    fun provideWeightEntryDao(database: AppDatabase): WeightEntryDao {
        return database.weightEntryDao()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideTextToSpeechManager(@ApplicationContext context: Context): TextToSpeechManager {
        return TextToSpeechManager(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}

