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

    private val _uiState =
        MutableStateFlow<StudyUiState>(StudyUiState.Loading)

    val uiState = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = StudyUiState.Loading

            delay(2000)

            _uiState.value = StudyUiState.Error(
                "服务器连接失败"
            )
        }
    }
}