package com.videocleaner.di

import android.content.Context
import androidx.room.Room
import com.videocleaner.data.local.dao.DuplicateGroupDao
import com.videocleaner.data.local.dao.VideoDao
import com.videocleaner.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO instances.
 * All are singletons since database connections are expensive to create.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development; use proper migrations in production
            .build()
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: AppDatabase): VideoDao = database.videoDao()

    @Provides
    @Singleton
    fun provideDuplicateGroupDao(database: AppDatabase): DuplicateGroupDao =
        database.duplicateGroupDao()
}
