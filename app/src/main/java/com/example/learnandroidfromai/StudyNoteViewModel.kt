package com.example.learnandroidfromai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyNoteUiState(
    val inputText: String = "",
    val savedText: String = ""
)

class StudyNoteViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val fileStore =
        StudyNoteFileStore(application)

    private val _uiState =
        MutableStateFlow(StudyNoteUiState())

    val uiState = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.update {
            it.copy(inputText = value)
        }
    }

    fun save() {
        viewModelScope.launch {
            val text = _uiState.value.inputText

            fileStore.save(text)

//            _uiState.update {
//                it.copy(savedText = text)
//            }
        }
    }

    fun read() {
        viewModelScope.launch {
            val text = fileStore.read()

            _uiState.update {
                it.copy(savedText = text)
            }
        }
    }
}