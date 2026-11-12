package com.liang.fridgerecipe.model

/** 菜谱里的一条用料，可选料不参与"能不能做"的判断，只影响匹配度 */
data class RecipeIngredient(
    val name: String,
    val amount: String = "适量",
    val optional: Boolean = false
)

/**
 * 菜谱。数据来自 assets/recipes.json，首次启动时导入 Room。
 */
data class Recipe(
    val id: Long = 0L,
    val name: String,
    val emoji: String = "🍲",
    val category: String = "家常菜",
    val minutes: Int = 20,
    val difficulty: Int = 1,
    val steps: List<String> = emptyList(),
    val tips: String = "",
    val ingredients: List<RecipeIngredient> = emptyList()
) {
    val requiredIngredients: List<RecipeIngredient>
        get() = ingredients.filter { !it.optional }

    val difficultyText: String
        get() = when (difficulty) {
            1 -> "简单"
            2 -> "适中"
            else -> "有点难"
        }

    val minutesText: String
        get() = "${minutes} 分钟"
}
