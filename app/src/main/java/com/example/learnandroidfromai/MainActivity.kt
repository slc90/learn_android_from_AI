package com.example.learnandroidfromai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import com.example.learnandroidfromai.ui.theme.Learn_android_from_AITheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val todoViewModel: TodoViewModel = viewModel()
            val todoUiState by todoViewModel.uiState.collectAsState()
            var todoIdText by remember {
                mutableStateOf("2")
            }

            Learn_android_from_AITheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Android 学习")
                            }
                        )
                    }
                ) { innerPadding ->
//                    Stage3App(
//                        modifier = Modifier.padding(innerPadding)
//                    )
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        TextField(
                            value = todoIdText,
                            onValueChange = {
                                todoIdText = it
                            },
                            label = {
                                Text("Todo id")
                            }
                        )

                        Button(
                            onClick = {
                                val id = todoIdText.toIntOrNull()

                                if (id != null) {
                                    todoViewModel.loadTodo(id)
                                }
                            }
                        ) {
                            Text("加载")
                        }

                        Text(
                            text = when {
                                todoUiState.isLoading -> "正在加载..."
                                todoUiState.errorMessage != null ->
                                    "请求失败：${todoUiState.errorMessage}"
                                else -> {
                                    val todo = todoUiState.todo

                                    if (todo != null) {
                                        "id=${todo.id}\n${todo.title}"
                                    } else {
                                        "没有数据"
                                    }
                                }
                            }
                        )

                        Button(
                            onClick = {
                                todoViewModel.createTodo()
                            }
                        ) {
                            Text("创建 Todo")
                        }

                        Button(
                            onClick = {
                                todoViewModel.loadTodosByUser(1)
                            }
                        ) {
                            Text("加载用户 1 的 Todo")
                        }

                        Text(
                            "数量：${todoUiState.todos.size}"
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(todoUiState.todos) { todo ->
                                Text(
                                    text = "${todo.id}. ${todo.title}",
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}