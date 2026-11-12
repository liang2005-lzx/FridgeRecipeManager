package com.liang.fridgerecipe.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.liang.fridgerecipe.model.FoodCategory
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.model.Recipe
import com.liang.fridgerecipe.model.RecipeIngredient

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val addedAt: Long,
    val expiresAt: Long,
    val note: String,
    val photoPath: String?
) {
    fun toDomain(): Ingredient = Ingredient(
        id = id,
        name = name,
        category = FoodCategory.fromName(category),
        quantity = quantity,
        unit = unit,
        addedAt = addedAt,
        expiresAt = expiresAt,
        note = note,
        photoPath = photoPath
    )

    companion object {
        fun fromDomain(item: Ingredient): IngredientEntity = IngredientEntity(
            id = item.id,
            name = item.name.trim(),
            category = item.category.name,
            quantity = item.quantity,
            unit = item.unit,
            addedAt = item.addedAt,
            expiresAt = item.expiresAt,
            note = item.note,
            photoPath = item.photoPath
        )
    }
}

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String,
    val category: String,
    val minutes: Int,
    val difficulty: Int,
    val stepsJson: String,
    val tips: String
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId")]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recipeId: Long,
    val name: String,
    val amount: String,
    val optional: Boolean
)

/** 一对多查询结果：一个菜谱 + 它的全部用料 */
data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<RecipeIngredientEntity>
) {
    fun toDomain(): Recipe = Recipe(
        id = recipe.id,
        name = recipe.name,
        emoji = recipe.emoji,
        category = recipe.category,
        minutes = recipe.minutes,
        difficulty = recipe.difficulty,
        steps = parseSteps(recipe.stepsJson),
        tips = recipe.tips,
        ingredients = ingredients.map {
            RecipeIngredient(name = it.name, amount = it.amount, optional = it.optional)
        }
    )

    companion object {
        private val gson = Gson()
        private val stepListType = object : TypeToken<List<String>>() { }.type

        fun parseSteps(json: String): List<String> = try {
            if (json.isBlank()) emptyList()
            else gson.fromJson<List<String>>(json, stepListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        fun toJson(steps: List<String>): String = gson.toJson(steps)
    }
}
