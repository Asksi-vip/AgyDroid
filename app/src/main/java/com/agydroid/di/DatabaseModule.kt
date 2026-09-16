package com.agydroid.di

import android.content.Context
import androidx.room.Room
import com.agydroid.data.local.AgyDroidDatabase
import com.agydroid.data.local.dao.BuildDao
import com.agydroid.data.local.dao.MessageDao
import com.agydroid.data.local.dao.ProjectDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AgyDroidDatabase {
        return Room.databaseBuilder(
            context,
            AgyDroidDatabase::class.java,
            "agydroid_database.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProjectDao(database: AgyDroidDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideMessageDao(database: AgyDroidDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideBuildDao(database: AgyDroidDatabase): BuildDao = database.buildDao()
}
