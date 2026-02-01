package cn.whc.launcher.di

import android.content.Context
import androidx.room.Room
import cn.whc.launcher.data.dao.AppDao
import cn.whc.launcher.data.dao.BlacklistDao
import cn.whc.launcher.data.dao.DailyStatsDao
import cn.whc.launcher.data.database.LauncherDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase {
        return Room.databaseBuilder(
            context,
            LauncherDatabase::class.java,
            LauncherDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDao(database: LauncherDatabase): AppDao {
        return database.appDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatsDao(database: LauncherDatabase): DailyStatsDao {
        return database.dailyStatsDao()
    }

    @Provides
    @Singleton
    fun provideBlacklistDao(database: LauncherDatabase): BlacklistDao {
        return database.blacklistDao()
    }
}
