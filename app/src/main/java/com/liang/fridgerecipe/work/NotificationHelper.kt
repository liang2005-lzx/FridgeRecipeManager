package com.liang.fridgerecipe.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.ui.MainActivity
import com.liang.fridgerecipe.util.DateUtils

/**
 * 过期提醒的通知封装。
 * 权限被拒绝时不抛异常，只返回 false，页面用应用内提示条降级。
 */
object NotificationHelper {

    const val CHANNEL_ID = "channel_expiry"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "食材过期提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每天提醒临期和已过期的食材"
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** 发一条汇总通知：有已过期的先说过期的，其次说临期的 */
    fun showExpiryReminder(context: Context, items: List<Ingredient>): Boolean {
        if (items.isEmpty()) return false
        if (!canPostNotification(context)) return false

        ensureChannel(context)

        val expired = items.filter { ExpiryAnalyzer.daysLeft(it.expiresAt) < 0 }
        val urgent = items.filter {
            val days = ExpiryAnalyzer.daysLeft(it.expiresAt)
            days >= 0 && days <= ExpiryAnalyzer.URGENT_DAYS
        }

        val title = when {
            expired.isNotEmpty() -> "有 ${expired.size} 样食材已经过期"
            else -> "有 ${urgent.size} 样食材今明到期"
        }

        val lines = items.take(5).joinToString("\n") { item ->
            "• ${item.name}（${DateUtils.daysLeftText(ExpiryAnalyzer.daysLeft(item.expiresAt))}）"
        }
        val content = if (items.size > 5) "$lines\n…共 ${items.size} 样待处理" else lines

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }
}
