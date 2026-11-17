package com.liang.fridgerecipe.data.remote

import com.liang.fridgerecipe.model.Recipe
import com.liang.fridgerecipe.model.RecipeIngredient

/**
 * 远程菜谱 JSON 的 DTO，字段与 assets/recipes.json 保持一致，
 * 这样"内置菜谱"和"云端菜谱"能用同一份数据格式，答辩时也好解释。
 */
data class RemoteRecipe(
    val name: String? = null,
    val emoji: String? = null,
    val category: String? = null,
    val minutes: Int? = null,
    val difficulty: Int? = null,
    val tips: String? = null,
    val steps: List<String>? = null,
    val ingredients: List<RemoteIngredient>? = null
) {
    fun toRecipe(): Recipe = Recipe(
        id = 0L,
        name = name.orEmpty().trim(),
        emoji = emoji ?: "🍲",
        category = category ?: "家常菜",
        minutes = minutes ?: 20,
        difficulty = difficulty ?: 1,
        tips = tips.orEmpty(),
        steps = steps.orEmpty(),
        ingredients = ingredients.orEmpty().mapNotNull { item ->
            val itemName = item.name?.trim().orEmpty()
            if (itemName.isEmpty()) null
            else RecipeIngredient(
                name = itemName,
                amount = item.amount ?: "适量",
                optional = item.optional ?: false
            )
        }
    )
}

data class RemoteIngredient(
    val name: String? = null,
    val amount: String? = null,
    val optional: Boolean? = null
)

/** DeepSeek 返回体里只取需要的字段 */
data class AiChatResponse(
    val choices: List<AiChoice>? = null
)

data class AiChoice(
    val message: AiMessage? = null
)

data class AiMessage(
    val content: String? = null
)
