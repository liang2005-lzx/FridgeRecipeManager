package com.liang.fridgerecipe.model

/**
 * 食材分类。defaultShelfLifeDays 用于估算"新鲜度风险"，
 * 数值是常温存放的参考保质期，用户可以在录入时覆盖。
 */
enum class FoodCategory(val label: String, val emoji: String, val defaultShelfLifeDays: Int) {
    VEGETABLE("蔬菜", "🥬", 7),
    FRUIT("水果", "🍎", 10),
    MEAT("肉类", "🥩", 4),
    SEAFOOD("水产", "🐟", 2),
    EGG_DAIRY("蛋奶", "🥚", 14),
    GRAIN("主食干货", "🍚", 60),
    FROZEN("冷冻食品", "🧊", 120),
    CONDIMENT("调味品", "🧂", 180),
    OTHER("其他", "🍱", 15);

    companion object {
        fun fromName(name: String?): FoodCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
