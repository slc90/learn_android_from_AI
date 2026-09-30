package com.example.learnandroidfromai.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateTodoRequest(
    val userId: Int,
    val title: String,
    val completed: Boolean
)