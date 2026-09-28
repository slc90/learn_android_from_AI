package com.example.learnandroidfromai

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Stage3UiState(
    val text: String = "",
    val isStarted: Boolean = false,
    val isLoading: Boolean = false,
    val isReviewMode: Boolean = false
)

class Stage3ViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow(Stage3UiState())

    val uiState = _uiState.asStateFlow()

    fun onTextChange(text: String) {
        _uiState.update { oldState ->
            oldState.copy(text = text)
        }
    }

    fun onStartClick() {
        _uiState.update { oldState ->
            oldState.copy(
                isStarted = true
            )
        }
    }

    fun onLoadingClick() {
        _uiState.update { oldState ->
            oldState.copy(
                isLoading = !oldState.isLoading
            )
        }
    }

    fun onReviewModeChange(checked: Boolean) {
        _uiState.update { oldState ->
            oldState.copy(
                isReviewMode = !oldState.isReviewMode
            )
        }
    }
}