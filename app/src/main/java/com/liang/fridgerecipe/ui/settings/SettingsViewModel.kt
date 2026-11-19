package com.liang.fridgerecipe.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liang.fridgerecipe.data.FridgeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository.get(application)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 3)
    val messages = _messages.asSharedFlow()

    fun importDemoIngredients() {
        viewModelScope.launch {
            runCatching { repository.insertDemoIngredients() }
                .onSuccess { _messages.emit("已导入 7 样示例食材") }
                .onFailure { _messages.emit("导入失败：${it.message}") }
        }
    }

    fun restoreRecipes() {
        viewModelScope.launch {
            runCatching { repository.seedRecipes(force = true) }
                .onSuccess { _messages.emit("内置菜谱已恢复") }
                .onFailure { _messages.emit("恢复失败：${it.message}") }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            runCatching { repository.clearAll() }
                .onSuccess { _messages.emit("数据已清空") }
                .onFailure { _messages.emit("清空失败：${it.message}") }
        }
    }
}
