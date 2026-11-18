package com.liang.fridgerecipe

import android.app.Application
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.util.PreferenceStore
import com.liang.fridgerecipe.work.NotificationHelper
import com.liang.fridgerecipe.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FridgeApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.ensureChannel(this)

        val prefs = PreferenceStore(this)
        if (prefs.notificationsEnabled) {
            ReminderScheduler.schedule(this, prefs.reminderHour, prefs.reminderMinute)
        }

        // 首次启动导入内置菜谱数据，保证菜谱页永远有内容
        appScope.launch {
            runCatching { FridgeRepository.get(this@FridgeApp).seedRecipes() }
        }
    }
}
