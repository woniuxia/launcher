package cn.whc.launcher.util

import cn.whc.launcher.data.model.AppInfo

/**
 * 应用搜索工具类
 */
object SearchHelper {

    /**
     * 搜索应用
     * 匹配优先级: 名称 > 拼音全拼 > 拼音首字母 > 包名
     */
    fun searchApps(
        query: String,
        allApps: List<AppInfo>,
        enablePinyin: Boolean = true
    ): List<AppInfo> {
        if (query.isBlank()) return emptyList()

        val lowerQuery = query.lowercase().trim()

        return allApps.filter { app ->
            val name = app.displayName.lowercase()

            // 匹配名称
            if (name.contains(lowerQuery)) return@filter true

            // 匹配拼音
            if (enablePinyin) {
                val pinyin = PinyinHelper.toPinyin(app.displayName)
                if (pinyin.contains(lowerQuery)) return@filter true

                val initials = PinyinHelper.getInitials(app.displayName)
                if (initials.contains(lowerQuery)) return@filter true
            }

            // 匹配包名
            app.packageName.lowercase().contains(lowerQuery)
        }.sortedByDescending { it.score }
    }

    /**
     * 高亮匹配文本 (返回匹配的起始和结束位置)
     */
    fun findMatchRanges(text: String, query: String): List<IntRange> {
        if (query.isBlank()) return emptyList()

        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        val ranges = mutableListOf<IntRange>()

        var startIndex = 0
        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) break
            ranges.add(index until (index + lowerQuery.length))
            startIndex = index + 1
        }

        return ranges
    }
}
