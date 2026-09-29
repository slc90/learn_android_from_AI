package com.example.learnandroidfromai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Delete
import androidx.room.Update

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: TaskEntity)

    @Query("SELECT * FROM tasks")
    fun getAll(): Flow<List<TaskEntity>>

    @Delete
    suspend fun delete(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)


    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getById(taskId: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE completed = :completed")
    fun getTasksByCompleted(completed: Boolean): Flow<List<TaskEntity>>
}