package com.liang.fridgerecipe.domain

import com.liang.fridgerecipe.model.FoodCategory
import com.liang.fridgerecipe.model.Ingredient
import java.util.Calendar

/** 保质期状态：决定列表颜色、排序和提醒策略 */
enum class ExpiryStatus(val label: String) {
    EXPIRED("已过期"),
    URGENT("今明到期"),
    SOON("快到期"),
    FRESH("新鲜")
}

data class ExpiryInfo(
    val daysLeft: Long,
    val status: ExpiryStatus,
    val risk: Int
)

data class CategoryCount(
    val category: FoodCategory,
    val count: Int
)

data class FridgeStats(
    val total: Int = 0,
    val expired: Int = 0,
    val urgent: Int = 0,
    val soon: Int = 0,
    val fresh: Int = 0,
    val wasteRate: Int = 0,
    val byCategory: List<CategoryCount> = emptyList()
) {
    val warningCount: Int get() = expired + urgent
}

/**
 * 自定义业务逻辑①：临期分析。
 *
 * 这里刻意不用"毫秒直接相减"，而是先把时间归零到当天 0 点再算天数差，
 * 这样才能保证"今天到期"永远显示 0 天，而不会因为录入时间不同变成 0.9 天。
 */
object ExpiryAnalyzer {

    const val URGENT_DAYS = 2L
    const val SOON_DAYS = 5L

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    fun startOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /** 剩余天数：今天到期 = 0，昨天到期 = -1 */
    fun daysLeft(expiresAt: Long, now: Long = System.currentTimeMillis()): Long {
        val diff = startOfDay(expiresAt) - startOfDay(now)
        return Math.floorDiv(diff, MILLIS_PER_DAY)
    }

    fun statusOf(daysLeft: Long): ExpiryStatus = when {
        daysLeft < 0 -> ExpiryStatus.EXPIRED
        daysLeft <= URGENT_DAYS -> ExpiryStatus.URGENT
        daysLeft <= SOON_DAYS -> ExpiryStatus.SOON
        else -> ExpiryStatus.FRESH
    }

    fun statusWeight(status: ExpiryStatus): Int = when (status) {
        ExpiryStatus.EXPIRED -> 0
        ExpiryStatus.URGENT -> 1
        ExpiryStatus.SOON -> 2
        ExpiryStatus.FRESH -> 3
    }

    /**
     * 新鲜度风险分（0-100）：把"剩余天数 / 该品类参考保质期"折算成比例。
     * 临期再额外加权，避免"保质期 180 天的调料只剩 2 天却只有 1 分"这种失真。
     */
    fun riskScore(item: Ingredient, daysLeft: Long = daysLeft(item.expiresAt)): Int {
        if (daysLeft < 0) return 100
        val shelfLife = item.category.defaultShelfLifeDays.toDouble().coerceAtLeast(1.0)
        val ratio = (daysLeft.toDouble() / shelfLife).coerceIn(0.0, 1.0)
        var risk = ((1.0 - ratio) * 100).toInt()
        if (daysLeft <= URGENT_DAYS) risk = maxOf(risk, 85)
        return risk.coerceIn(0, 100)
    }

    fun info(item: Ingredient, now: Long = System.currentTimeMillis()): ExpiryInfo {
        val days = daysLeft(item.expiresAt, now)
        return ExpiryInfo(days, statusOf(days), riskScore(item, days))
    }

    /** 列表排序：过期 → 今明到期 → 快到期 → 新鲜，同组内按剩余天数升序 */
    fun sortByPriority(list: List<Ingredient>, now: Long = System.currentTimeMillis()): List<Ingredient> =
        list.sortedWith(
            compareBy<Ingredient> {
                statusWeight(statusOf(daysLeft(it.expiresAt, now)))
            }.thenBy { daysLeft(it.expiresAt, now) }
                .thenBy { it.name }
        )

    /** 看板用的聚合统计：过期率就是"预计浪费率" */
    fun stats(list: List<Ingredient>, now: Long = System.currentTimeMillis()): FridgeStats {
        var expired = 0
        var urgent = 0
        var soon = 0
        var fresh = 0
        val categoryCounter = linkedMapOf<FoodCategory, Int>()

        list.forEach { item ->
            when (statusOf(daysLeft(item.expiresAt, now))) {
                ExpiryStatus.EXPIRED -> expired++
                ExpiryStatus.URGENT -> urgent++
                ExpiryStatus.SOON -> soon++
                ExpiryStatus.FRESH -> fresh++
            }
            categoryCounter[item.category] = (categoryCounter[item.category] ?: 0) + 1
        }

        val wasteRate = if (list.isEmpty()) 0 else (expired * 100) / list.size
        val byCategory = categoryCounter.entries
            .sortedByDescending { it.value }
            .map { CategoryCount(it.key, it.value) }

        return FridgeStats(
            total = list.size,
            expired = expired,
            urgent = urgent,
            soon = soon,
            fresh = fresh,
            wasteRate = wasteRate,
            byCategory = byCategory
        )
    }
}
