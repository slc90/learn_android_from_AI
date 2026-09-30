package com.example.learnandroidfromai.data

import com.example.learnandroidfromai.model.Todo

interface TodoRepository {

    suspend fun getTodo(id: Int): Todo

    suspend fun createTodo(
        userId: Int,
        title: String,
        completed: Boolean
    ): Todo

    suspend fun getTodosByUser(
        userId: Int
    ): List<Todo>
}