package com.liang.fridgerecipe.model

import java.util.Locale

/**
 * 冰箱里的一条食材记录。
 * expiresAt / addedAt 都是毫秒时间戳（取当天 0 点，便于按"天数"计算）。
 */
data class Ingredient(
    val id: Long = 0L,
    val name: String,
    val category: FoodCategory = FoodCategory.OTHER,
    val quantity: Double = 1.0,
    val unit: String = "份",
    val addedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val photoPath: String? = null
) {
    /** 1.0 -> "1"，1.5 -> "1.5" */
    val quantityText: String
        get() = if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", quantity)
        }
}
