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
import com.example.learnandroidfromai.model.Person

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer =
            (application as LearnAndroidApplication).appContainer

        val nativeBridge = NativeBridge()
        val person = Person("mdrs", 18)

        val result = nativeBridge.describePerson(person)
        println("describePerson result = $result")

        val nativePerson = nativeBridge.createPerson()
        println("createPerson result = $nativePerson")

        val people = listOf(
            Person("Alice", 20),
            Person("Bob", 30)
        )

        val peopleResult = nativeBridge.describePeople(people)
        println("describePeople result = $peopleResult")

        val nativePeople = nativeBridge.createPeople()
        println("createPeople result = $nativePeople")

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