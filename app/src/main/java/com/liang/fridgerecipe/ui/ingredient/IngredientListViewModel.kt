package com.liang.fridgerecipe.ui.ingredient

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.domain.ExpiryStatus
import com.liang.fridgerecipe.model.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class IngredientFilter(val label: String) {
    ALL("全部"),
    EXPIRING("临期"),
    EXPIRED("已过期"),
    FRESH("新鲜")
}

sealed interface IngredientListUiState {
    data object Loading : IngredientListUiState

    data class Content(
        val items: List<Ingredient>,
        val totalCount: Int,
        val warningCount: Int,
        val filter: IngredientFilter,
        val keyword: String
    ) : IngredientListUiState

    data class Error(val message: String) : IngredientListUiState
}

class IngredientListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)
    private val keyword = MutableStateFlow("")
    private val filter = MutableStateFlow(IngredientFilter.ALL)
    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<IngredientListUiState> =
        combine(
            repository.observeIngredients(),
            keyword,
            filter,
            retryTick
        ) { all, word, currentFilter, _ ->
            val now = System.currentTimeMillis()
            val searched = if (word.isBlank()) all else {
                all.filter { it.name.contains(word.trim(), ignoreCase = true) }
            }
            val filtered = searched.filter { item ->
                val status = ExpiryAnalyzer.statusOf(ExpiryAnalyzer.daysLeft(item.expiresAt, now))
                when (currentFilter) {
                    IngredientFilter.ALL -> true
                    IngredientFilter.EXPIRING -> status == ExpiryStatus.URGENT || status == ExpiryStatus.SOON
                    IngredientFilter.EXPIRED -> status == ExpiryStatus.EXPIRED
                    IngredientFilter.FRESH -> status == ExpiryStatus.FRESH
                }
            }
            IngredientListUiState.Content(
                items = ExpiryAnalyzer.sortByPriority(filtered, now),
                totalCount = all.size,
                warningCount = ExpiryAnalyzer.stats(all, now).warningCount,
                filter = currentFilter,
                keyword = word
            ) as IngredientListUiState
        }.catch { error ->
            emit(IngredientListUiState.Error(error.message ?: "读取本地数据失败"))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IngredientListUiState.Loading
        )

    fun setKeyword(text: String) {
        keyword.value = text
    }

    fun setFilter(value: IngredientFilter) {
        filter.value = value
    }

    fun retry() {
        retryTick.value += 1
    }
}
