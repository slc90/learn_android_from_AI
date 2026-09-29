package com.example.learnandroidfromai

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator

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
    viewModel: SetupViewModel = viewModel()
) {
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
    }
}

@Composable
fun StudyScreen(
    topic: String,
    onBack: () -> Unit,
    viewModel: StudyViewModel = viewModel()
) {
    Column {
        SectionCard(
            header = {
                Text("学习主题")
            },
            content = {
                // TODO：显示 topic
            }
        )

        SectionCard(
            header = {
                Text("计数")
            },
            content = {
                // TODO：
                // 显示 count
                // 再放一个 +1 Button
            }
        )

        Button(
            onClick = onBack
        ) {
            Text("返回")
        }
    }
}