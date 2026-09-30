package com.example.learnandroidfromai.data

import com.example.learnandroidfromai.data.remote.CreateTodoRequest
import com.example.learnandroidfromai.data.remote.TodoApiService
import com.example.learnandroidfromai.data.remote.TodoDto
import com.example.learnandroidfromai.model.Todo

class NetworkTodoRepository(
    private val api: TodoApiService
) : TodoRepository {

    override suspend fun getTodo(id: Int): Todo {
        val response = api.getTodo(id)

        if (!response.isSuccessful) {
            throw IllegalStateException(
                "HTTP ${response.code()}"
            )
        }

        val dto = response.body()
            ?: throw IllegalStateException("响应数据为空")

        return dto.toTodo()
    }

    override suspend fun createTodo(
        userId: Int,
        title: String,
        completed: Boolean
    ): Todo {
        val request = CreateTodoRequest(
            userId = userId,
            title = title,
            completed = completed
        )

        val dto =
            api.createTodo(request)

        return dto.toTodo()
    }

    override suspend fun getTodosByUser(
        userId: Int
    ): List<Todo> {
        val response =
            api.getTodosByUser(userId)

        if (!response.isSuccessful) {
            throw IllegalStateException(
                "HTTP ${response.code()}"
            )
        }

        return response.body()
            ?.map { it.toTodo() }
            ?: emptyList()
    }
}

private fun TodoDto.toTodo(): Todo {
    return Todo(
        userId = userId,
        id = id,
        title = title,
        completed = completed
    )
}