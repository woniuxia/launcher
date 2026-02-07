package cn.whc.launcher.util

import cn.whc.launcher.data.entity.AppEntity
import cn.whc.launcher.data.entity.BlacklistEntity
import timber.log.Timber

/**
 * 通用扩展函数和工具函数
 */

// ==================== ComponentKey 扩展 ====================

/**
 * 生成 componentKey (格式: "packageName/activityName")
 */
val AppEntity.componentKey: String
    get() = "$packageName/$activityName"

val BlacklistEntity.componentKey: String
    get() = "$packageName/$activityName"

/**
 * 从 componentKey 解析 packageName 和 activityName
 */
fun parseComponentKey(componentKey: String): Pair<String, String>? {
    val parts = componentKey.split("/", limit = 2)
    return if (parts.size == 2) parts[0] to parts[1] else null
}

// ==================== 安全执行 ====================

/**
 * 安全执行代码块，捕获异常并记录日志
 * @param tag 日志标签
 * @param message 错误信息描述
 * @param fallback 异常时的默认返回值
 * @param block 执行块
 */
inline fun <T> safeExecute(
    tag: String,
    message: String,
    fallback: T,
    block: () -> T
): T {
    return try {
        block()
    } catch (e: Exception) {
        Timber.tag(tag).e(e, message)
        fallback
    }
}

/**
 * 安全执行可空返回值的代码块
 */
inline fun <T> safeExecuteOrNull(
    tag: String,
    message: String,
    block: () -> T?
): T? {
    return try {
        block()
    } catch (e: Exception) {
        Timber.tag(tag).e(e, message)
        null
    }
}

/**
 * 带回退的安全执行 (主逻辑失败时尝试备用逻辑)
 */
inline fun <T> safeExecuteWithFallback(
    tag: String,
    primaryMessage: String,
    fallbackMessage: String,
    fallbackValue: T,
    primary: () -> T,
    fallback: () -> T
): T {
    return try {
        primary()
    } catch (e: Exception) {
        Timber.tag(tag).w(e, primaryMessage)
        try {
            fallback()
        } catch (e2: Exception) {
            Timber.tag(tag).e(e2, fallbackMessage)
            fallbackValue
        }
    }
}

// ==================== 黑名单过滤 ====================

/**
 * 从应用列表中过滤掉黑名单和隐藏的应用
 */
fun List<AppEntity>.filterByBlacklist(blacklistKeys: Set<String>): List<AppEntity> {
    return filter { it.componentKey !in blacklistKeys && !it.isHidden }
}

/**
 * 将黑名单列表转换为 componentKey 集合
 */
fun List<BlacklistEntity>.toComponentKeySet(): Set<String> {
    return map { it.componentKey }.toSet()
}
