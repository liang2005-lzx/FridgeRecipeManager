package com.liang.fridgerecipe.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 时间格式统一放这里，避免每个界面各写一套 */
object DateUtils {

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val monthDayFormat = SimpleDateFormat("M 月 d 日", Locale.CHINA)
    private val weekdayFormat = SimpleDateFormat("M/d 周W", Locale.CHINA)

    fun toDayText(millis: Long): String = dayFormat.format(Date(millis))

    fun toMonthDayText(millis: Long): String = monthDayFormat.format(Date(millis))

    fun todayStart(): Long = startOfDay(Calendar.getInstance().timeInMillis)

    fun startOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun parseDay(text: String): Long? = runCatching { dayFormat.parse(text)?.time }.getOrNull()

    /** 剩余天数 -> 可读文案 */
    fun daysLeftText(daysLeft: Long): String = when {
        daysLeft < 0 -> "已过期 ${-daysLeft} 天"
        daysLeft == 0L -> "今天到期"
        daysLeft == 1L -> "明天到期"
        else -> "还剩 $daysLeft 天"
    }

    /** MaterialDatePicker 用的是 UTC 毫秒，直接传本地时间会差一天，这里做一次换算 */
    fun toPickerMillis(localDayMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localDayMillis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        return utc.timeInMillis
    }

    fun fromPickerMillis(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        val local = Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }
        return local.timeInMillis
    }

    fun addDays(localDayMillis: Long, days: Int): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = localDayMillis }
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }
}
