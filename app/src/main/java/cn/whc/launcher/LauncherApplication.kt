package cn.whc.launcher

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cn.whc.launcher.util.PinyinHelper
import cn.whc.launcher.worker.DataCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class LauncherApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // 初始化日志 (仅 Debug 模式)
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        }

        // 初始化拼音库
        PinyinHelper.init(this)

        // 延迟注册定期清理任务（避免 WorkManager 内部数据库创建阻塞主线程）
        android.os.Handler(mainLooper).post { scheduleDataCleanup() }
    }

    private fun scheduleDataCleanup() {
        val request = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DataCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
