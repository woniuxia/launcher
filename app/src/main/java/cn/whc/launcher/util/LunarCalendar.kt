package cn.whc.launcher.util

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 农历计算工具类
 * 基于寿星万年历算法实现
 */
object LunarCalendar {
    // 农历月份名称
    private val LUNAR_MONTHS = arrayOf(
        "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
    )

    // 农历日期名称
    private val LUNAR_DAYS = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    // 天干
    private val TIAN_GAN = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")

    // 地支
    private val DI_ZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

    // 生肖
    private val SHENG_XIAO = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    // 农历数据表 (1900-2100)
    // 每个元素表示一年的农历信息，用16位表示
    // 前4位表示闰月月份(0表示无闰月)，后12位表示每月大小(1表示大月30天，0表示小月29天)
    private val LUNAR_INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x16a95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520
    )

    data class LunarDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val isLeapMonth: Boolean,
        val yearGanZhi: String,
        val monthGanZhi: String,
        val dayGanZhi: String,
        val zodiac: String
    ) {
        fun getMonthStr(): String = if (isLeapMonth) "闰${LUNAR_MONTHS[month - 1]}月" else "${LUNAR_MONTHS[month - 1]}月"
        fun getDayStr(): String = LUNAR_DAYS[day - 1]
        fun getFullDateStr(): String = "${getMonthStr()}${getDayStr()}"
    }

    /**
     * 将公历日期转换为农历日期
     */
    fun solarToLunar(date: LocalDate): LunarDate {
        return solarToLunar(date.year, date.monthValue, date.dayOfMonth)
    }

    fun solarToLunar(solarYear: Int, solarMonth: Int, solarDay: Int): LunarDate {
        // 计算与1900年1月31日(农历1900年正月初一)相差的天数
        val baseDate = LocalDate.of(1900, 1, 31)
        val targetDate = LocalDate.of(solarYear, solarMonth, solarDay)
        var offset = (targetDate.toEpochDay() - baseDate.toEpochDay()).toInt()

        // 计算农历年份
        var lunarYear = 1900
        var daysInYear: Int
        while (lunarYear < 2100 && offset > 0) {
            daysInYear = getLunarYearDays(lunarYear)
            if (offset < daysInYear) break
            offset -= daysInYear
            lunarYear++
        }

        // 获取闰月
        val leapMonth = getLeapMonth(lunarYear)
        var isLeap = false

        // 计算农历月份
        var lunarMonth = 1
        var daysInMonth: Int
        var monthCount = 0
        while (monthCount < 13 && lunarMonth <= 12) {
            if (leapMonth > 0 && lunarMonth == leapMonth && !isLeap) {
                // 先处理正常月份
                daysInMonth = getLunarMonthDays(lunarYear, lunarMonth)
                if (offset < daysInMonth) break
                offset -= daysInMonth
                monthCount++
                // 接下来处理闰月
                isLeap = true
                daysInMonth = getLeapDays(lunarYear)
                if (offset < daysInMonth) break
                offset -= daysInMonth
                isLeap = false
                monthCount++
                lunarMonth++
            } else {
                daysInMonth = getLunarMonthDays(lunarYear, lunarMonth)
                if (offset < daysInMonth) break
                offset -= daysInMonth
                monthCount++
                lunarMonth++
            }
        }

        // 确保月份在有效范围内
        if (lunarMonth > 12) lunarMonth = 12

        val lunarDay = offset + 1

        // 计算干支
        val yearGanZhi = getYearGanZhi(lunarYear)
        val monthGanZhi = getMonthGanZhi(lunarYear, lunarMonth)
        val dayGanZhi = getDayGanZhi(solarYear, solarMonth, solarDay)
        val zodiac = SHENG_XIAO[(lunarYear - 4) % 12]

        return LunarDate(
            year = lunarYear,
            month = lunarMonth,
            day = lunarDay,
            isLeapMonth = isLeap,
            yearGanZhi = yearGanZhi,
            monthGanZhi = monthGanZhi,
            dayGanZhi = dayGanZhi,
            zodiac = zodiac
        )
    }

    /**
     * 获取农历年的总天数
     */
    private fun getLunarYearDays(year: Int): Int {
        var sum = 348 // 12个月，每月29天
        val info = LUNAR_INFO[year - 1900]

        // 累加大月的额外天数（检查高12位，每位代表一个月）
        var mask = 0x8000
        repeat(12) {
            if (info and mask != 0) sum++
            mask = mask shr 1
        }

        // 加上闰月天数
        return sum + getLeapDays(year)
    }

    /**
     * 获取农历年闰月的天数
     */
    private fun getLeapDays(year: Int): Int {
        return if (getLeapMonth(year) != 0) {
            if (LUNAR_INFO[year - 1900] and 0x10000 != 0) 30 else 29
        } else 0
    }

    /**
     * 获取农历年闰哪个月 (0表示无闰月)
     */
    private fun getLeapMonth(year: Int): Int {
        return LUNAR_INFO[year - 1900] and 0xf
    }

    /**
     * 获取农历某月的天数
     */
    private fun getLunarMonthDays(year: Int, month: Int): Int {
        return if (LUNAR_INFO[year - 1900] and (0x10000 shr month) != 0) 30 else 29
    }

    /**
     * 获取年干支
     */
    private fun getYearGanZhi(year: Int): String {
        val gan = (year - 4) % 10
        val zhi = (year - 4) % 12
        return "${TIAN_GAN[gan]}${DI_ZHI[zhi]}"
    }

    /**
     * 获取月干支
     */
    private fun getMonthGanZhi(year: Int, month: Int): String {
        val firstGan = (year - 4) % 10
        val monthGan = (firstGan * 2 + month) % 10
        val monthZhi = (month + 1) % 12
        return "${TIAN_GAN[monthGan]}${DI_ZHI[monthZhi]}"
    }

    /**
     * 获取日干支
     */
    private fun getDayGanZhi(year: Int, month: Int, day: Int): String {
        val date = LocalDate.of(year, month, day)
        val baseDate = LocalDate.of(1900, 1, 1)
        val offset = (date.toEpochDay() - baseDate.toEpochDay()).toInt()

        val gan = (offset + 10) % 10
        val zhi = offset % 12
        return "${TIAN_GAN[gan]}${DI_ZHI[zhi]}"
    }

    /**
     * 获取当前农历日期字符串
     */
    fun getTodayLunarString(): String {
        val lunar = solarToLunar(LocalDate.now())
        return lunar.getFullDateStr()
    }
}
