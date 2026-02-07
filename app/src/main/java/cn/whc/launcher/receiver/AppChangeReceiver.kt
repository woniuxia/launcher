package cn.whc.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.whc.launcher.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 应用安装/卸载/更新广播接收器
 */
@AndroidEntryPoint
class AppChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appRepository: AppRepository

    companion object {
        private const val TAG = "AppChangeReceiver"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.tag(TAG).e(throwable, "Error handling package change")
    }

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        Timber.tag(TAG).d("Received action: %s for package: %s", intent.action, packageName)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            Timber.tag(TAG).d("App installed: %s", packageName)
                            appRepository.onAppInstalled(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            Timber.tag(TAG).d("App uninstalled: %s", packageName)
                            appRepository.onAppUninstalled(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        Timber.tag(TAG).d("App updated: %s", packageName)
                        appRepository.onAppUpdated(packageName)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to handle package change: %s", packageName)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
