package com.example.learnandroidfromai.ui.todo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier


@Composable
fun TodoScreen(
    uiState: TodoUiState,
    onLoadTodo: (Int) -> Unit,
    onCreateTodo: () -> Unit,
    onLoadTodosByUser: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var todoIdText by remember {
        mutableStateOf("2")
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        TextField(
            value = todoIdText,
            onValueChange = { todoIdText = it },
            label = { Text("Todo id") }
        )

        Button(
            onClick = {
                todoIdText.toIntOrNull()?.let(onLoadTodo)
            }
        ) {
            Text("加载")
        }

        Text(
            text = when {
                uiState.isLoading ->
                    "正在加载..."

                uiState.errorMessage != null ->
                    "请求失败：${uiState.errorMessage}"

                uiState.todo != null ->
                    "id=${uiState.todo.id}\n${uiState.todo.title}"

                else ->
                    "没有数据"
            }
        )

        Button(
            onClick = onCreateTodo
        ) {
            Text("创建 Todo")
        }

        Button(
            onClick = {
                onLoadTodosByUser(1)
            }
        ) {
            Text("加载用户 1 的 Todo")
        }

        Text("数量：${uiState.todos.size}")

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.todos) { todo ->
                Text(
                    text = "${todo.id}. ${todo.title}",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}