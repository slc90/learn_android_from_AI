package com.example.learnandroidfromai

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Checkbox

@Serializable
data object SetupRoute : NavKey

@Serializable
data class StudyRoute(
    val topic: String
) : NavKey

@Composable
fun Stage3App(modifier: Modifier = Modifier) {

    val backStack = rememberNavBackStack(SetupRoute)

    val navigateBack: () -> Unit = {
        backStack.removeLastOrNull()
    }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = {
            navigateBack()
        },

        entryProvider = entryProvider {

            entry<SetupRoute> {
                SetupScreen(
                    onNavigateToStudy = { topic ->
                        backStack.add(
                            StudyRoute(topic = topic)
                        )
                    }
                )
            }

            entry<StudyRoute> { route ->
                StudyScreen(
                    topic = route.topic,
                    onBack = navigateBack
                )
            }
        }
    )
}

@Composable
fun SetupScreen(
    onNavigateToStudy: (String) -> Unit,
    viewModel: SetupViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    studyNoteViewModel: StudyNoteViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val taskUiState by
    taskViewModel.uiState.collectAsStateWithLifecycle()

    var taskTitle by remember {
        mutableStateOf("")
    }

    val settingsUiState by
    settingsViewModel.uiState.collectAsStateWithLifecycle()

    val noteUiState by
    studyNoteViewModel.uiState.collectAsStateWithLifecycle()

    Column {
        OutlinedTextField(
            value = viewModel.text,
            onValueChange = viewModel::onTextChange
        )

        Button(
            onClick = {
                onNavigateToStudy(viewModel.text)
            }
        ) {
            Text("开始学习")
        }

        Row {
            Text("显示学习提示")

            Switch(
                checked = settingsUiState.showStudyTip,
                onCheckedChange = settingsViewModel::setShowStudyTip
            )
        }

        OutlinedTextField(
            value = noteUiState.inputText,
            onValueChange = studyNoteViewModel::onInputChange,
            label = {
                Text("学习笔记")
            }
        )

        Row {
            Button(
                onClick = studyNoteViewModel::save
            ) {
                Text("保存")
            }

            Button(
                onClick = studyNoteViewModel::read
            ) {
                Text("读取")
            }
        }

        Text(
            text = "读取结果：${noteUiState.savedText}"
        )

        SectionCard(
            header = {
                Text("Room 任务")
            },
            content = {

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = {
                        taskTitle = it
                    },
                    label = {
                        Text("任务名称")
                    }
                )

                Row {
                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                taskViewModel.addTask(taskTitle)
                                taskTitle = ""
                            }
                        }
                    ) {
                        Text("添加")
                    }
                }

                Row {
                    Text("仅显示未完成")

                    Switch(
                        checked = taskUiState.showIncompleteOnly,
                        onCheckedChange = taskViewModel::setShowIncompleteOnly
                    )
                }

                if (taskUiState.tasks.isEmpty()) {
                    Text("暂无任务")
                } else {
                    taskUiState.tasks.forEach { task ->
                        Row {
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = {
                                    taskViewModel.toggleCompleted(task)
                                }
                            )

                            Text(
                                text = "${task.id}. ${task.title}"
                            )

                            Button(
                                onClick = {
                                    taskViewModel.deleteTask(task)
                                }
                            ) {
                                Text("删除")
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun StudyScreen(
    topic: String,
    onBack: () -> Unit,
    viewModel: StudyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column {

        SectionCard(
            header = {
                Text("当前 UI 状态")
            },
            content = {
                when (val state = uiState) {

                    StudyUiState.Loading -> {
                        Text("正在加载...")
                    }

                    StudyUiState.Empty -> {
                        Text("暂无内容")
                    }

                    is StudyUiState.Error -> {
                        Column {
                            Text("错误：${state.message}")

                            Button(
                                onClick = viewModel::loadArticles
                            ) {
                                Text("重试")
                            }
                        }
                    }

                    is StudyUiState.Content -> {
                        Column {
                            state.articles.forEach { article ->
                                Text(article)
                            }
                        }
                    }
                }
            }
        )

        Button(onClick = onBack) {
            Text("返回")
        }
    }
}