package cn.whc.launcher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.whc.launcher.data.dao.AppDao
import cn.whc.launcher.data.dao.BlacklistDao
import cn.whc.launcher.data.dao.DailyStatsDao
import cn.whc.launcher.data.dao.GraylistDao
import cn.whc.launcher.data.dao.LaunchTimeDao
import cn.whc.launcher.data.entity.AppEntity
import cn.whc.launcher.data.entity.BlacklistEntity
import cn.whc.launcher.data.entity.DailyStatEntity
import cn.whc.launcher.data.entity.GraylistEntity
import cn.whc.launcher.data.entity.LaunchTimeEntity

@Database(
    entities = [
        AppEntity::class,
        DailyStatEntity::class,
        BlacklistEntity::class,
        GraylistEntity::class,
        LaunchTimeEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun blacklistDao(): BlacklistDao
    abstract fun graylistDao(): GraylistDao
    abstract fun launchTimeDao(): LaunchTimeDao

    companion object {
        const val DATABASE_NAME = "launcher_db"
    }
}
