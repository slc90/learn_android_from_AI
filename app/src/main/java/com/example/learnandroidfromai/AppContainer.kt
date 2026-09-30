package com.example.learnandroidfromai

import com.example.learnandroidfromai.data.NetworkTodoRepository
import com.example.learnandroidfromai.data.TodoRepository
import com.example.learnandroidfromai.data.remote.RetrofitClient

class AppContainer {

    val todoRepository: TodoRepository =
        NetworkTodoRepository(
            RetrofitClient.todoApi
        )
}