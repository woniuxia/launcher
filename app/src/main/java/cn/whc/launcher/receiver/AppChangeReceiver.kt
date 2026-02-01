package cn.whc.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.whc.launcher.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用安装/卸载/更新广播接收器
 */
@AndroidEntryPoint
class AppChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appRepository: AppRepository

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            appRepository.onAppInstalled(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            appRepository.onAppUninstalled(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        appRepository.onAppUpdated(packageName)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
