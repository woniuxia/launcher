package cn.whc.launcher.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.whc.launcher.data.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 数据清理 Worker
 * 定期清理30天前的统计数据
 */
@HiltWorker
class DataCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appRepository: AppRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            appRepository.cleanupOldStats()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "data_cleanup"
    }
}
