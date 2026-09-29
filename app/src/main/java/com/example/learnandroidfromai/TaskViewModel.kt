package com.example.learnandroidfromai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnandroidfromai.data.TaskRepository
import com.example.learnandroidfromai.data.local.AppDatabase
import com.example.learnandroidfromai.data.local.TaskEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest

data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val showIncompleteOnly: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val showIncompleteOnly =
        MutableStateFlow(false)

    private val _uiState =
        MutableStateFlow(TaskUiState())

    val uiState = _uiState.asStateFlow()

    private val repository = TaskRepository(
        AppDatabase
            .getInstance(application)
            .taskDao()
    )

    fun addTask(title: String) {
        viewModelScope.launch {
            repository.addTask(title)
        }
    }

    init {
        viewModelScope.launch {
            showIncompleteOnly
                .flatMapLatest { onlyIncomplete ->
                    if (onlyIncomplete) {
                        repository.getIncompleteTasks()
                    } else {
                        repository.getAllTasks()
                    }
                }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(tasks = tasks)
                    }
                }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleCompleted(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(
                task.copy(
                    completed = !task.completed
                )
            )
        }
    }

    fun setShowIncompleteOnly(value: Boolean) {
        showIncompleteOnly.value = value

        _uiState.update {
            it.copy(showIncompleteOnly = value)
        }
    }
}