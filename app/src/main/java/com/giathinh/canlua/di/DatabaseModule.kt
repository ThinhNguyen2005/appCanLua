package com.giathinh.canlua.di

import android.content.Context
import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.dao.ProfileDao
import com.giathinh.canlua.data.database.AppDatabase
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.util.TextToSpeechManager
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
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideRicePriceDao(database: AppDatabase): com.giathinh.canlua.data.dao.RicePriceDao {
        return database.ricePriceDao()
    }



    @Provides
    fun provideNewsArticleDao(database: AppDatabase): com.giathinh.canlua.data.dao.NewsArticleDao {
        return database.newsArticleDao()
    }

    @Provides
    fun provideDeletedCardDao(database: AppDatabase): com.giathinh.canlua.data.dao.DeletedCardDao {
        return database.deletedCardDao()
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

