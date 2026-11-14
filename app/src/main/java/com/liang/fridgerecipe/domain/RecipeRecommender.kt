package com.liang.fridgerecipe.domain

import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.model.Recipe

/**
 * 一个菜谱和当前冰箱的匹配结果（自选功能"按食材智能推荐菜谱"的核心产物）。
 */
data class RecipeMatch(
    val recipe: Recipe,
    val score: Int,
    val matchedCount: Int,
    val requiredCount: Int,
    val matchedNames: List<String> = emptyList(),
    val missingNames: List<String> = emptyList(),
    val expiredNames: List<String> = emptyList(),
    val reason: String = ""
) {
    val canCook: Boolean get() = missingNames.isEmpty() && expiredNames.isEmpty()

    val coverageText: String get() = "$matchedCount/$requiredCount 样主料"

    /** 缺料或过期料需要提醒时用红色 */
    val needAttention: Boolean get() = !canCook
}

/**
 * 自定义业务逻辑②：本地推荐算法（不依赖网络，纯规则打分）。
 *
 * 打分公式（满分 100）：
 *   食材覆盖率 = 命中的主料数 / 主料总数           → 最高 50 分
 *   临期消耗   = 命中食材中非"新鲜"状态的比例      → 最高 20 分（鼓励先吃要坏的）
 *   缺料惩罚   = 15 - 每缺一样主料 8 分，最低 0     → 最高 15 分
 *   快手加分   = ≤15 分钟 10 分 / ≤30 分钟 6 分 / 其他 3 分
 */
object RecipeRecommender {

    private val noiseChars = Regex("[\\s（）()、,，·]")

    private fun normalize(raw: String): String {
        var name = raw.replace(noiseChars, "")
        name = name.removePrefix("新鲜").removePrefix("冰冻")
        name = name.removeSuffix("适量").removeSuffix("少许")
        // "两个鸡蛋" 里的量词不影响识别
        name = name.replace(Regex("^(一|两|二|三|四|五|半)?(个|根|颗|把|块|片|条|只|袋|盒|勺)"), "")
        return name
    }

    /** 用"包含"判断命中，这样"小葱"和"葱"、"鸡蛋"和"土鸡蛋"都能对上 */
    fun isSameIngredient(a: String, b: String): Boolean {
        val x = normalize(a)
        val y = normalize(b)
        if (x.isEmpty() || y.isEmpty()) return false
        return x == y || x.contains(y) || y.contains(x)
    }

    fun recommend(
        recipes: List<Recipe>,
        stock: List<Ingredient>,
        now: Long = System.currentTimeMillis()
    ): List<RecipeMatch> {
        // 过期食材不再算"可用"，但仍参与"差一点就能做"的提示
        val available = stock.filter { ExpiryAnalyzer.daysLeft(it.expiresAt, now) >= 0 }
        return recipes
            .map { buildMatch(it, available, stock, now) }
            .sortedWith(compareByDescending<RecipeMatch> { it.score }.thenBy { it.recipe.name })
    }

    private fun buildMatch(
        recipe: Recipe,
        available: List<Ingredient>,
        allStock: List<Ingredient>,
        now: Long
    ): RecipeMatch {
        val required = recipe.ingredients.filter { !it.optional }
        val optional = recipe.ingredients.filter { it.optional }

        val matchedNames = mutableListOf<String>()
        val missingNames = mutableListOf<String>()
        val expiredNames = mutableListOf<String>()
        val matchedStock = mutableListOf<Ingredient>()
        var matchedRequired = 0

        required.forEach { need ->
            val hit = available.firstOrNull { isSameIngredient(it.name, need.name) }
            if (hit != null) {
                matchedRequired++
                matchedNames += need.name
                matchedStock += hit
            } else {
                val expiredHit = allStock.firstOrNull {
                    isSameIngredient(it.name, need.name) && ExpiryAnalyzer.daysLeft(it.expiresAt, now) < 0
                }
                if (expiredHit != null) {
                    expiredNames += need.name
                } else {
                    missingNames += need.name
                }
            }
        }

        optional.forEach { need ->
            val hit = available.firstOrNull { isSameIngredient(it.name, need.name) }
            if (hit != null) matchedNames += need.name
        }

        val coverage = if (required.isEmpty()) 1.0 else matchedRequired.toDouble() / required.size
        val urgentHits = matchedStock.count {
            ExpiryAnalyzer.statusOf(ExpiryAnalyzer.daysLeft(it.expiresAt, now)) != ExpiryStatus.FRESH
        }
        val urgency = if (matchedStock.isEmpty()) 0.0 else urgentHits.toDouble() / matchedStock.size
        val missingScore = maxOf(0, 15 - missingNames.size * 8)
        val speedScore = when {
            recipe.minutes <= 15 -> 10
            recipe.minutes <= 30 -> 6
            else -> 3
        }

        val score = (coverage * 50 + urgency * 20 + missingScore + speedScore)
            .toInt()
            .coerceIn(0, 100)

        val reason = when {
            expiredNames.isNotEmpty() && missingNames.isEmpty() ->
                "食材都有，但「${expiredNames.first()}」已过期，先检查一下"
            missingNames.isEmpty() && matchedStock.isNotEmpty() -> "食材齐全，可以直接开做"
            matchedRequired == 0 -> "主料都还没有，可以先去采购"
            missingNames.isNotEmpty() -> "还缺 ${missingNames.size} 样：${missingNames.take(3).joinToString("、")}"
            else -> "可以先看看做法"
        }

        return RecipeMatch(
            recipe = recipe,
            score = score,
            matchedCount = matchedRequired,
            requiredCount = required.size,
            matchedNames = matchedNames,
            missingNames = missingNames,
            expiredNames = expiredNames,
            reason = reason
        )
    }
}
