package com.example.learnandroidfromai.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnandroidfromai.data.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.learnandroidfromai.model.Todo

data class TodoUiState(
    val isLoading: Boolean = true,
    val todo: Todo? = null,
    val todos: List<Todo> = emptyList(),
    val errorMessage: String? = null
)
class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState = _uiState.asStateFlow()

    fun loadTodo(id: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val todo = repository.getTodo(id)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        todo = todo,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun createTodo() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val createdTodo = repository.createTodo(
                    userId = 1,
                    title = "我从 Android 发来的 Todo",
                    completed = false
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        todo = createdTodo
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun loadTodosByUser(userId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val todos = repository.getTodosByUser(userId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        todos = todos
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }
}