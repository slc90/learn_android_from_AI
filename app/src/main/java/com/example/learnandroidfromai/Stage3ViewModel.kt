package com.example.learnandroidfromai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

class StudyViewModel : ViewModel() {

    var count by mutableIntStateOf(0)
        private set

    fun increase() {
        count++
    }
}