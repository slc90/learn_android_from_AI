package com.example.learnandroidfromai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showStudyTip: Boolean = true
)

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsDataStore =
        SettingsDataStore(application)

    private val _uiState =
        MutableStateFlow(SettingsUiState())

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.showStudyTip.collect { value ->
                _uiState.update { oldState ->
                    oldState.copy(
                        showStudyTip = value
                    )
                }
            }
        }
    }

    fun setShowStudyTip(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowStudyTip(value)
        }
    }
}