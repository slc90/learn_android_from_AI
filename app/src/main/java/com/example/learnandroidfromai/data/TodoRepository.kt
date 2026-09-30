package com.example.learnandroidfromai.data

import com.example.learnandroidfromai.data.remote.CreateTodoRequest
import com.example.learnandroidfromai.data.remote.RetrofitClient
import com.example.learnandroidfromai.data.remote.TodoDto
import retrofit2.Response

class TodoRepository {

    suspend fun getTodo(id: Int): Response<TodoDto> {
        return RetrofitClient.todoApi.getTodo(id)
    }

    suspend fun createTodo(
        request: CreateTodoRequest
    ): TodoDto {
        return RetrofitClient.todoApi.createTodo(request)
    }

    suspend fun getTodosByUser(
        userId: Int
    ): Response<List<TodoDto>> {
        return RetrofitClient.todoApi.getTodosByUser(userId)
    }
}