package com.liang.fridgerecipe.util

import android.content.Context

/**
 * 轻量配置存储（提醒开关 / 提醒时间）。
 * 用 SharedPreferences 是因为这里只有两三个开关，不需要引入 DataStore。
 */
class PreferenceStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_MINUTE, value).apply()

    val reminderTimeText: String
        get() = "%02d:%02d".format(reminderHour, reminderMinute)

    /** 通知权限只主动弹一次，避免每次进来都骚扰用户 */
    var askedNotificationPermission: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    companion object {
        private const val PREFS_NAME = "fridge_prefs"
        private const val KEY_NOTIFY = "key_notify"
        private const val KEY_HOUR = "key_hour"
        private const val KEY_MINUTE = "key_minute"
        private const val KEY_ASKED = "key_asked_permission"
    }
}
