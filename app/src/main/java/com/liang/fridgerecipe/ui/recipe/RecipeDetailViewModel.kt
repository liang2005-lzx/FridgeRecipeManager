package com.liang.fridgerecipe.ui.recipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.domain.RecipeMatch
import com.liang.fridgerecipe.domain.RecipeRecommender
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.model.Recipe
import com.liang.fridgerecipe.model.RecipeIngredient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class StockState { AVAILABLE, EXPIRING, EXPIRED, MISSING }

data class IngredientStockLine(
    val ingredient: RecipeIngredient,
    val state: StockState,
    val stockName: String? = null
)

data class RecipeDetailUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    val match: RecipeMatch? = null,
    val lines: List<IngredientStockLine> = emptyList()
)

class RecipeDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)
    private val recipeId: Long = savedStateHandle.get<Long>("recipeId") ?: -1L

    val uiState: StateFlow<RecipeDetailUiState> =
        combine(
            repository.observeRecipes(),
            repository.observeIngredients()
        ) { recipes, stock ->
            val recipe = recipes.firstOrNull { it.id == recipeId }
            if (recipe == null) {
                RecipeDetailUiState(loading = false)
            } else {
                val match = RecipeRecommender.recommend(recipes, stock)
                    .firstOrNull { it.recipe.id == recipe.id }
                RecipeDetailUiState(
                    loading = false,
                    recipe = recipe,
                    match = match,
                    lines = buildLines(recipe.ingredients, stock)
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipeDetailUiState()
        )

    private fun buildLines(
        ingredients: List<RecipeIngredient>,
        stock: List<Ingredient>
    ): List<IngredientStockLine> {
        val now = System.currentTimeMillis()
        return ingredients.map { need ->
            val hit = stock.firstOrNull { RecipeRecommender.isSameIngredient(it.name, need.name) }
            when {
                hit == null -> IngredientStockLine(need, StockState.MISSING)
                ExpiryAnalyzer.daysLeft(hit.expiresAt, now) < 0 ->
                    IngredientStockLine(need, StockState.EXPIRED, hit.name)
                ExpiryAnalyzer.daysLeft(hit.expiresAt, now) <= ExpiryAnalyzer.URGENT_DAYS ->
                    IngredientStockLine(need, StockState.EXPIRING, hit.name)
                else -> IngredientStockLine(need, StockState.AVAILABLE, hit.name)
            }
        }
    }
}
