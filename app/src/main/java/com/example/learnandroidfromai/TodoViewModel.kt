package com.example.learnandroidfromai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnandroidfromai.data.TodoRepository
import com.example.learnandroidfromai.data.remote.CreateTodoRequest
import com.example.learnandroidfromai.data.remote.TodoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoUiState(
    val isLoading: Boolean = true,
    val todo: TodoDto? = null,
    val todos: List<TodoDto> = emptyList(),
    val errorMessage: String? = null
)

class TodoViewModel : ViewModel() {

    private val repository = TodoRepository()

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
                val response = repository.getTodo(id)

                if (response.isSuccessful) {
                    val todo = response.body()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todo = todo,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "HTTP ${response.code()}"
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "网络异常：${e.message}"
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
                val request = CreateTodoRequest(
                    userId = 1,
                    title = "我从 Android 发来的 Todo",
                    completed = false
                )

                val createdTodo = repository.createTodo(request)

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
                val response = repository.getTodosByUser(userId)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todos = response.body() ?: emptyList()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "HTTP ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "网络异常：${e.message}"
                    )
                }
            }
        }
    }
}