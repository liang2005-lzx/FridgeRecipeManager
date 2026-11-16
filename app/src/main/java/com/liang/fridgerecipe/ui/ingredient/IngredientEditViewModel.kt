package com.liang.fridgerecipe.ui.ingredient

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import com.liang.fridgerecipe.model.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EditState {
    data object Idle : EditState
    data object Saving : EditState
    data object Saved : EditState
    data class Error(val message: String) : EditState
}

class IngredientEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)

    private val _editing = MutableStateFlow<Ingredient?>(null)
    val editing: StateFlow<Ingredient?> = _editing.asStateFlow()

    private val _loadFinished = MutableStateFlow(false)
    val loadFinished: StateFlow<Boolean> = _loadFinished.asStateFlow()

    private val _saveState = MutableStateFlow<EditState>(EditState.Idle)
    val saveState: StateFlow<EditState> = _saveState.asStateFlow()

    fun load(id: Long) {
        if (id <= 0L) {
            _loadFinished.value = true
            return
        }
        viewModelScope.launch {
            _editing.value = runCatching { repository.ingredientById(id) }.getOrNull()
            _loadFinished.value = true
        }
    }

    fun save(item: Ingredient) {
        if (_saveState.value == EditState.Saving) return
        viewModelScope.launch {
            _saveState.value = EditState.Saving
            runCatching { repository.saveIngredient(item) }
                .onSuccess { _saveState.value = EditState.Saved }
                .onFailure { _saveState.value = EditState.Error(it.message ?: "保存失败，请重试") }
        }
    }

    fun resetSaveState() {
        _saveState.value = EditState.Idle
    }
}
