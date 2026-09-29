package com.example.learnandroidfromai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Stage3UiState(
    val isStarted: Boolean = false,
)

class Stage3ViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow(Stage3UiState())

    val uiState = _uiState.asStateFlow()


    fun onStartClick() {
        _uiState.update { oldState ->
            oldState.copy(
                isStarted = true
            )
        }
    }
}

class SetupViewModel : ViewModel() {

    var text by mutableStateOf("")
        private set

    fun onTextChange(value: String) {
        text = value
    }
}

sealed interface StudyUiState {

    data object Loading : StudyUiState

    data object Empty : StudyUiState

    data class Error(
        val message: String
    ) : StudyUiState

    data class Content(
        val articles: List<String>
    ) : StudyUiState
}

class StudyViewModel : ViewModel() {

    var uiState by mutableStateOf<StudyUiState>(
        StudyUiState.Loading
    )
        private set

    var count by mutableIntStateOf(0)
        private set

    init {
        loadArticles()
    }

    fun increase() {
        count++
    }

    fun loadArticles() {
        viewModelScope.launch {
            uiState = StudyUiState.Loading

            delay(2000)

            try {
                throw Exception("服务器连接失败")

                // 以后这里才会是真正的数据加载
            } catch (e: Exception) {
                uiState = StudyUiState.Error(
                    message = e.message ?: "未知错误"
                )
            }
        }
    }
}