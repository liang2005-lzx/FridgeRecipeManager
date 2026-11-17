package com.liang.fridgerecipe.ui.recipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.domain.RecipeMatch
import com.liang.fridgerecipe.domain.RecipeRecommender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface RecipeListUiState {
    data object Loading : RecipeListUiState

    data class Content(
        val matches: List<RecipeMatch>,
        val totalCount: Int,
        val cookableCount: Int,
        val onlyCookable: Boolean
    ) : RecipeListUiState

    data class Error(val message: String) : RecipeListUiState
}

class RecipeListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)
    private val onlyCookable = MutableStateFlow(false)
    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<RecipeListUiState> =
        combine(
            repository.observeRecipes(),
            repository.observeIngredients(),
            onlyCookable,
            retryTick
        ) { recipes, stock, only, _ ->
            val matches = RecipeRecommender.recommend(recipes, stock)
            val cookable = matches.count { it.canCook }
            val visible = if (only) matches.filter { it.canCook } else matches
            RecipeListUiState.Content(
                matches = visible,
                totalCount = recipes.size,
                cookableCount = cookable,
                onlyCookable = only
            ) as RecipeListUiState
        }.catch { error ->
            emit(RecipeListUiState.Error(error.message ?: "菜谱加载失败"))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipeListUiState.Loading
        )

    fun setOnlyCookable(value: Boolean) {
        onlyCookable.value = value
    }

    fun retry() {
        retryTick.value += 1
    }
}
