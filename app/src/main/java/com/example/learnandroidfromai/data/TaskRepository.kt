package com.example.learnandroidfromai.data

import com.example.learnandroidfromai.data.local.TaskDao
import com.example.learnandroidfromai.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {

    suspend fun addTask(title: String) {
        taskDao.insert(
            TaskEntity(title = title)
        )
    }

    fun getAllTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAll()
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
    }

    fun getIncompleteTasks(): Flow<List<TaskEntity>> {
        return taskDao.getTasksByCompleted(false)
    }
}