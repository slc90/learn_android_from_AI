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
import com.example.learnandroidfromai.ui.todo.TodoScreen
import com.example.learnandroidfromai.ui.todo.TodoViewModel
import com.example.learnandroidfromai.ui.todo.TodoViewModelFactory

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer =
            (application as LearnAndroidApplication).appContainer

        setContent {
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

                    val todoViewModel: TodoViewModel = viewModel(
                        factory = TodoViewModelFactory(
                            appContainer.todoRepository
                        )
                    )

                    val todoUiState by todoViewModel.uiState.collectAsState()
                    TodoScreen(
                        uiState = todoUiState,
                        onLoadTodo = todoViewModel::loadTodo,
                        onCreateTodo = todoViewModel::createTodo,
                        onLoadTodosByUser = todoViewModel::loadTodosByUser,
                        modifier = Modifier.padding(innerPadding)
                    )
                    }
                }
            }
        }
}