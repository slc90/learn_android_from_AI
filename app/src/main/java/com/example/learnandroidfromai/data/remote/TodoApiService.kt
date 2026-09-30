package com.example.learnandroidfromai.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface TodoApiService {

    @GET("todos/{id}")
    suspend fun getTodo(
        @Path("id") id: Int
    ): Response<TodoDto>

    @POST("todos")
    suspend fun createTodo(
        @Body request: CreateTodoRequest
    ): TodoDto

    @GET("todos")
    suspend fun getTodosByUser(
        @Query("userId") userId: Int
    ): Response<List<TodoDto>>
}