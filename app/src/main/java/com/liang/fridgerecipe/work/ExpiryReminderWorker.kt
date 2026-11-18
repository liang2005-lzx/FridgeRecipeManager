package com.liang.fridgerecipe.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.util.PreferenceStore
import kotlinx.coroutines.flow.first

/**
 * 后台任务（硬能力）：每天检查一次库存里临期/过期的食材并提醒。
 * 用 WorkManager 而不是 AlarmManager，是为了让系统统一调度、不耗电。
 */
class ExpiryReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val prefs = PreferenceStore(applicationContext)
        if (!prefs.notificationsEnabled) return@runCatching Result.success()

        val repository = FridgeRepository.get(applicationContext)
        val ingredients = repository.observeIngredients().first()

        val targets = ExpiryAnalyzer.sortByPriority(ingredients).filter {
            val days = ExpiryAnalyzer.daysLeft(it.expiresAt)
            days <= ExpiryAnalyzer.URGENT_DAYS
        }

        if (targets.isNotEmpty()) {
            NotificationHelper.showExpiryReminder(applicationContext, targets)
        }
        Result.success()
    }.getOrElse {
        // 数据库读失败等异常不让任务直接崩，交给 WorkManager 重试
        Result.retry()
    }
}
