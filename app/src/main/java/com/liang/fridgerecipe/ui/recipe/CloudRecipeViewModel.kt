package com.liang.fridgerecipe.ui.recipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.data.remote.AiRecipeService
import com.liang.fridgerecipe.data.remote.RecipeSyncService
import com.liang.fridgerecipe.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data class Success(val count: Int) : SyncState
    data class Error(val message: String) : SyncState
}

sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Result(val recipe: Recipe) : AiState
    data class Error(val message: String) : AiState
}

/**
 * 联网功能的状态机：云同步 + AI 生成。
 * 所有网络请求都在 viewModelScope 里跑，页面销毁自动取消，不会泄漏。
 */
class CloudRecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)
    private val syncService = RecipeSyncService()
    private val aiService = AiRecipeService()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    fun syncFromCloud() {
        if (_syncState.value == SyncState.Loading) return
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            val result = syncService.fetchRecipes()
            result.onSuccess { recipes ->
                val saved = runCatching { repository.replaceAllRecipes(recipes) }
                    .getOrElse { error ->
                        _syncState.value = SyncState.Error(error.message ?: "写入本地数据库失败")
                        return@launch
                    }
                _syncState.value = SyncState.Success(saved)
            }.onFailure { error ->
                _syncState.value = SyncState.Error(error.message ?: "网络不可用，已继续使用本地菜谱")
            }
        }
    }

    fun generateByAi() {
        if (_aiState.value == AiState.Loading) return
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            val stock = runCatching { repository.observeIngredients().first() }.getOrDefault(emptyList())
            aiService.generateRecipe(stock)
                .onSuccess { recipe -> _aiState.value = AiState.Result(recipe) }
                .onFailure { error ->
                    _aiState.value = AiState.Error(error.message ?: "AI 生成失败，请稍后再试")
                }
        }
    }

    /** AI 结果满意后写入本地菜谱库 */
    fun saveAiRecipe(recipe: Recipe) {
        viewModelScope.launch {
            runCatching { repository.addRecipe(recipe) }
                .onSuccess { _syncState.value = SyncState.Success(1) }
                .onFailure { _aiState.value = AiState.Error(it.message ?: "保存失败") }
        }
    }

    fun resetAiState() {
        _aiState.value = AiState.Idle
    }
}
