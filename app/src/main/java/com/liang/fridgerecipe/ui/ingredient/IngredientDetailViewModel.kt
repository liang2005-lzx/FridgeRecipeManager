package com.liang.fridgerecipe.ui.ingredient

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.domain.RecipeMatch
import com.liang.fridgerecipe.domain.RecipeRecommender
import com.liang.fridgerecipe.model.Ingredient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IngredientDetailUiState(
    val loading: Boolean = true,
    val ingredient: Ingredient? = null,
    val recommendations: List<RecipeMatch> = emptyList(),
    val error: String? = null
)

sealed interface IngredientDetailEvent {
    data object Deleted : IngredientDetailEvent
}

class IngredientDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)
    private val ingredientId: Long = savedStateHandle.get<Long>("ingredientId") ?: -1L

    private val _events = MutableSharedFlow<IngredientDetailEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun deleteIngredient() {
        viewModelScope.launch {
            runCatching { repository.deleteIngredient(ingredientId) }
            _events.emit(IngredientDetailEvent.Deleted)
        }
    }

    val uiState: StateFlow<IngredientDetailUiState> =
        combine(
            repository.observeIngredients(),
            repository.observeRecipes()
        ) { ingredients, recipes ->
            val item = ingredients.firstOrNull { it.id == ingredientId }
            if (item == null) {
                IngredientDetailUiState(loading = false, ingredient = null)
            } else {
                val matches = RecipeRecommender.recommend(recipes, ingredients)
                    .filter { match ->
                        match.matchedNames.any { RecipeRecommender.isSameIngredient(it, item.name) }
                    }
                    .take(3)
                IngredientDetailUiState(
                    loading = false,
                    ingredient = item,
                    recommendations = matches
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IngredientDetailUiState()
        )
}
