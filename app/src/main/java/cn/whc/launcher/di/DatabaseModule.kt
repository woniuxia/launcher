package cn.whc.launcher.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.whc.launcher.data.dao.AppDao
import cn.whc.launcher.data.dao.BlacklistDao
import cn.whc.launcher.data.dao.DailyStatsDao
import cn.whc.launcher.data.dao.GraylistDao
import cn.whc.launcher.data.dao.LaunchTimeDao
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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS launch_time_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    package_name TEXT NOT NULL,
                    activity_name TEXT NOT NULL,
                    launch_timestamp INTEGER NOT NULL,
                    time_of_day_minutes INTEGER NOT NULL,
                    FOREIGN KEY (package_name, activity_name)
                        REFERENCES apps(package_name, activity_name)
                        ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_launch_time_records_time_window ON launch_time_records(time_of_day_minutes, launch_timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_launch_time_records_component ON launch_time_records(package_name, activity_name)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_launch_time_records_timestamp ON launch_time_records(launch_timestamp)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS graylist (
                    package_name TEXT NOT NULL,
                    activity_name TEXT NOT NULL,
                    added_at INTEGER NOT NULL,
                    PRIMARY KEY (package_name, activity_name),
                    FOREIGN KEY (package_name, activity_name)
                        REFERENCES apps(package_name, activity_name)
                        ON DELETE CASCADE
                )
            """)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase {
        return Room.databaseBuilder(
            context,
            LauncherDatabase::class.java,
            LauncherDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
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

    @Provides
    @Singleton
    fun provideGraylistDao(database: LauncherDatabase): GraylistDao {
        return database.graylistDao()
    }

    @Provides
    @Singleton
    fun provideLaunchTimeDao(database: LauncherDatabase): LaunchTimeDao {
        return database.launchTimeDao()
    }
}
