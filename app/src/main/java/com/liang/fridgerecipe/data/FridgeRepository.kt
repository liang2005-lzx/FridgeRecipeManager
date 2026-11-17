package com.liang.fridgerecipe.data

import android.content.Context
import com.google.gson.Gson
import com.liang.fridgerecipe.data.local.AppDatabase
import com.liang.fridgerecipe.data.local.IngredientEntity
import com.liang.fridgerecipe.data.local.RecipeEntity
import com.liang.fridgerecipe.data.local.RecipeIngredientEntity
import com.liang.fridgerecipe.data.local.RecipeWithIngredients
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.model.Recipe
import com.liang.fridgerecipe.model.RecipeIngredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 单一数据入口：UI 层只依赖 Repository，不直接碰 Room。
 */
class FridgeRepository private constructor(
    private val context: Context,
    private val db: AppDatabase
) {

    private val ingredientDao = db.ingredientDao()
    private val recipeDao = db.recipeDao()
    private val gson = Gson()

    fun observeIngredients(): Flow<List<Ingredient>> =
        ingredientDao.observeAll().map { list -> list.map(IngredientEntity::toDomain) }

    fun observeRecipes(): Flow<List<Recipe>> =
        recipeDao.observeAll().map { list -> list.map(RecipeWithIngredients::toDomain) }

    suspend fun ingredientById(id: Long): Ingredient? =
        withContext(Dispatchers.IO) { ingredientDao.getById(id)?.toDomain() }

    suspend fun recipeById(id: Long): Recipe? =
        withContext(Dispatchers.IO) { recipeDao.getById(id)?.toDomain() }

    /** id == 0 视为新增，否则更新 */
    suspend fun saveIngredient(item: Ingredient): Long = withContext(Dispatchers.IO) {
        val entity = IngredientEntity.fromDomain(item)
        if (item.id == 0L) {
            ingredientDao.insert(entity)
        } else {
            ingredientDao.update(entity)
            item.id
        }
    }

    suspend fun deleteIngredient(id: Long) = withContext(Dispatchers.IO) {
        ingredientDao.deleteById(id)
    }

    suspend fun countIngredients(): Int = withContext(Dispatchers.IO) { ingredientDao.count() }

    suspend fun clearIngredients() = withContext(Dispatchers.IO) { ingredientDao.clear() }

    /** 首次启动把 assets/recipes.json 导入数据库；force = true 时先清空再导 */
    suspend fun seedRecipes(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!force && recipeDao.count() > 0) return@withContext
        if (force) recipeDao.clearRecipes()

        val raw = runCatching {
            context.assets.open("recipes.json").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return@withContext

        val seeds = runCatching {
            gson.fromJson(raw, Array<SeedRecipe>::class.java)?.toList().orEmpty()
        }.getOrElse { emptyList() }

        seeds.forEach { seed ->
            val name = seed.name ?: return@forEach
            val recipeId = recipeDao.insertRecipe(
                RecipeEntity(
                    name = name,
                    emoji = seed.emoji ?: "🍲",
                    category = seed.category ?: "家常菜",
                    minutes = seed.minutes ?: 20,
                    difficulty = seed.difficulty ?: 1,
                    stepsJson = RecipeWithIngredients.toJson(seed.steps.orEmpty()),
                    tips = seed.tips.orEmpty()
                )
            )
            val ingredients = seed.ingredients.orEmpty().map { item ->
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    name = item.name.orEmpty(),
                    amount = item.amount ?: "适量",
                    optional = item.optional ?: false
                )
            }
            if (ingredients.isNotEmpty()) recipeDao.insertIngredients(ingredients)
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        ingredientDao.clear()
        recipeDao.clearRecipes()
    }

    /**
     * 联网同步：用云端菜谱替换本地菜谱库。
     * 食材库存不动，只换菜谱；写入失败会抛出异常，由调用方提示"离线模式"。
     */
    suspend fun replaceAllRecipes(recipes: List<Recipe>): Int = withContext(Dispatchers.IO) {
        recipeDao.clearRecipes()
        recipes.forEach { insertRecipeInternal(it) }
        recipes.size
    }

    /** AI 生成的菜谱保存进本地库，返回新 id */
    suspend fun addRecipe(recipe: Recipe): Long = withContext(Dispatchers.IO) {
        insertRecipeInternal(recipe)
    }

    private suspend fun insertRecipeInternal(recipe: Recipe): Long {
        val recipeId = recipeDao.insertRecipe(
            RecipeEntity(
                name = recipe.name,
                emoji = recipe.emoji,
                category = recipe.category,
                minutes = recipe.minutes,
                difficulty = recipe.difficulty,
                stepsJson = RecipeWithIngredients.toJson(recipe.steps),
                tips = recipe.tips
            )
        )
        val ingredients = recipe.ingredients.map { item ->
            RecipeIngredientEntity(
                recipeId = recipeId,
                name = item.name,
                amount = item.amount,
                optional = item.optional
            )
        }
        if (ingredients.isNotEmpty()) recipeDao.insertIngredients(ingredients)
        return recipeId
    }

    /** 一键导入几条示例食材，方便演示和看板 */
    suspend fun insertDemoIngredients() = withContext(Dispatchers.IO) {
        val now = Calendar.getInstance().timeInMillis
        val demo = listOf(
            Triple("番茄", com.liang.fridgerecipe.model.FoodCategory.VEGETABLE, 3L),
            Triple("鸡蛋", com.liang.fridgerecipe.model.FoodCategory.EGG_DAIRY, 1L),
            Triple("西兰花", com.liang.fridgerecipe.model.FoodCategory.VEGETABLE, 4L),
            Triple("鸡胸肉", com.liang.fridgerecipe.model.FoodCategory.MEAT, 2L),
            Triple("土豆", com.liang.fridgerecipe.model.FoodCategory.VEGETABLE, 10L),
            Triple("牛奶", com.liang.fridgerecipe.model.FoodCategory.EGG_DAIRY, -1L),
            Triple("小葱", com.liang.fridgerecipe.model.FoodCategory.VEGETABLE, 2L)
        )
        demo.forEachIndexed { index, (name, category, shelf) ->
            ingredientDao.insert(
                IngredientEntity(
                    name = name,
                    category = category.name,
                    quantity = (index + 1).toDouble(),
                    unit = if (category == com.liang.fridgerecipe.model.FoodCategory.MEAT) "块" else "份",
                    addedAt = now,
                    expiresAt = now + shelf * DAY_MILLIS,
                    note = "示例数据",
                    photoPath = null
                )
            )
        }
    }

    private data class SeedIngredient(
        val name: String? = null,
        val amount: String? = null,
        val optional: Boolean? = null
    )

    private data class SeedRecipe(
        val name: String? = null,
        val emoji: String? = null,
        val category: String? = null,
        val minutes: Int? = null,
        val difficulty: Int? = null,
        val tips: String? = null,
        val steps: List<String>? = null,
        val ingredients: List<SeedIngredient>? = null
    )

    companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000

        @Volatile
        private var instance: FridgeRepository? = null

        fun get(context: Context): FridgeRepository =
            instance ?: synchronized(this) {
                instance ?: FridgeRepository(
                    context.applicationContext,
                    AppDatabase.get(context.applicationContext)
                ).also { instance = it }
            }
    }
}
