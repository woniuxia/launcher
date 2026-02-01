package cn.whc.launcher.util

import net.sourceforge.pinyin4j.PinyinHelper as P4j
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

/**
 * 拼音工具类 - 使用 Pinyin4j 库
 */
object PinyinHelper {
    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    fun init(context: android.content.Context) {
        // Pinyin4j 不需要初始化
    }

    /**
     * 获取字符串首字母 (A-Z 或 #)
     */
    fun getFirstLetter(text: String): String {
        if (text.isEmpty()) return "#"
        val first = text.first()
        return when {
            first.isLetter() && first.code < 128 -> first.uppercaseChar().toString()
            isChinese(first) -> {
                try {
                    val pinyinArray = P4j.toHanyuPinyinStringArray(first, format)
                    if (!pinyinArray.isNullOrEmpty()) {
                        pinyinArray[0].first().uppercaseChar().toString()
                    } else "#"
                } catch (e: Exception) {
                    "#"
                }
            }
            else -> "#"
        }
    }

    /**
     * 将中文转换为拼音全拼
     */
    fun toPinyin(text: String): String {
        return text.map { char ->
            if (isChinese(char)) {
                try {
                    val pinyinArray = P4j.toHanyuPinyinStringArray(char, format)
                    pinyinArray?.firstOrNull() ?: char.toString()
                } catch (e: Exception) {
                    char.toString()
                }
            } else {
                char.lowercase().toString()
            }
        }.joinToString("")
    }

    /**
     * 获取拼音首字母
     */
    fun getInitials(text: String): String {
        return text.map { char ->
            if (isChinese(char)) {
                try {
                    val pinyinArray = P4j.toHanyuPinyinStringArray(char, format)
                    if (!pinyinArray.isNullOrEmpty()) {
                        pinyinArray[0].first()
                    } else char
                } catch (e: Exception) {
                    char
                }
            } else if (char.isLetter()) {
                char.lowercaseChar()
            } else {
                char
            }
        }.joinToString("")
    }

    /**
     * 判断字符是否为中文
     */
    private fun isChinese(char: Char): Boolean {
        val ub = Character.UnicodeBlock.of(char)
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
    }
}
