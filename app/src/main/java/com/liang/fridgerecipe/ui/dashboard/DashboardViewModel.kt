package com.liang.fridgerecipe.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.domain.FridgeStats
import com.liang.fridgerecipe.model.Ingredient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Content(
        val stats: FridgeStats,
        val priority: List<Ingredient>
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)

    val uiState: StateFlow<DashboardUiState> = repository.observeIngredients()
        .map { ingredients ->
            val now = System.currentTimeMillis()
            val stats = ExpiryAnalyzer.stats(ingredients, now)
            val priority = ExpiryAnalyzer.sortByPriority(ingredients, now)
                .filter {
                    ExpiryAnalyzer.daysLeft(it.expiresAt, now) <= ExpiryAnalyzer.URGENT_DAYS
                }
                .take(5)
            DashboardUiState.Content(stats, priority) as DashboardUiState
        }
        .catch { error -> emit(DashboardUiState.Error(error.message ?: "统计失败")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )
}
