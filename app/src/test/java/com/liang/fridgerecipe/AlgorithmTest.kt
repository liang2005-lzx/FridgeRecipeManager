package com.liang.fridgerecipe

import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.domain.ExpiryStatus
import com.liang.fridgerecipe.domain.RecipeRecommender
import com.liang.fridgerecipe.model.FoodCategory
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.model.Recipe
import com.liang.fridgerecipe.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 两个自定义算法模块的单元测试（纯逻辑，不依赖 Android 环境）。
 */
class AlgorithmTest {

    private val day = 24L * 60 * 60 * 1000

    private fun ingredient(name: String, expiresOffsetDays: Long): Ingredient {
        val today = ExpiryAnalyzer.startOfDay(System.currentTimeMillis())
        return Ingredient(
            name = name,
            category = FoodCategory.OTHER,
            quantity = 1.0,
            unit = "份",
            addedAt = today - day,
            expiresAt = today + expiresOffsetDays * day
        )
    }

    private fun recipe(id: Long, name: String, needed: List<String>): Recipe = Recipe(
        id = id,
        name = name,
        emoji = "🍲",
        category = "家常菜",
        minutes = 10,
        difficulty = 1,
        tips = "",
        steps = listOf("步骤一"),
        ingredients = needed.map { RecipeIngredient(it, "适量", false) }
    )

    @Test
    fun daysLeft_todayIsZero() {
        val today = ExpiryAnalyzer.startOfDay(System.currentTimeMillis())
        assertEquals(0L, ExpiryAnalyzer.daysLeft(today, today))
    }

    @Test
    fun daysLeft_yesterdayIsMinusOne() {
        val today = ExpiryAnalyzer.startOfDay(System.currentTimeMillis())
        assertEquals(-1L, ExpiryAnalyzer.daysLeft(today - day, today))
    }

    @Test
    fun status_followsThresholds() {
        assertEquals(ExpiryStatus.EXPIRED, ExpiryAnalyzer.statusOf(-1))
        assertEquals(ExpiryStatus.URGENT, ExpiryAnalyzer.statusOf(0))
        assertEquals(ExpiryStatus.URGENT, ExpiryAnalyzer.statusOf(2))
        assertEquals(ExpiryStatus.SOON, ExpiryAnalyzer.statusOf(3))
        assertEquals(ExpiryStatus.FRESH, ExpiryAnalyzer.statusOf(10))
    }

    @Test
    fun recommend_fullMatchScoresHigherThanPartialMatch() {
        val stock = listOf(ingredient("番茄", 5), ingredient("鸡蛋", 5))
        val fullMatch = recipe(1, "番茄炒蛋", listOf("番茄", "鸡蛋"))
        val partial = recipe(2, "番茄牛腩", listOf("番茄", "牛肉"))

        val matches = RecipeRecommender.recommend(listOf(fullMatch, partial), stock)
        val a = matches.first { it.recipe.id == 1L }
        val b = matches.first { it.recipe.id == 2L }

        assertTrue(a.score > b.score)
        assertTrue(a.canCook)
        assertFalse(b.canCook)
        assertTrue(b.missingNames.contains("牛肉"))
    }

    @Test
    fun recommend_ignoresExpiredStock() {
        val stock = listOf(ingredient("番茄", -1))
        val target = recipe(1, "番茄炒蛋", listOf("番茄"))

        val match = RecipeRecommender.recommend(listOf(target), stock).first()

        assertFalse(match.canCook)
        assertTrue(match.expiredNames.contains("番茄"))
    }

    @Test
    fun fuzzyNameMatch_toleratesSuffix() {
        assertTrue(RecipeRecommender.isSameIngredient("鸡蛋", "土鸡蛋"))
        assertTrue(RecipeRecommender.isSameIngredient("番茄 2个", "番茄"))
        assertFalse(RecipeRecommender.isSameIngredient("番茄", "牛肉"))
    }
}
