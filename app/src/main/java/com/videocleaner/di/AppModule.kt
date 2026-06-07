package com.videocleaner.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * App-level Hilt module.
 * Additional bindings can be added here as needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
