package com.example.learnandroidfromai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.learnandroidfromai.ui.theme.Learn_android_from_AITheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    Stage3Screen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


}

@Composable
fun Stage3Screen(modifier: Modifier = Modifier,
                 viewModel: Stage3ViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Stage3Content(
        modifier = modifier,
        uiState = uiState,
        onTextChange = viewModel::onTextChange,
        onStartClick = viewModel::onStartClick,
        onLoadingClick = viewModel::onLoadingClick
    )
}

@Composable
fun Stage3Content(
    modifier: Modifier = Modifier,
    uiState: Stage3UiState,
    onTextChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onLoadingClick: () -> Unit,
) {
    val canStart = uiState.text.isNotBlank()

    var showDetails by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OutlinedTextField(
            value = uiState.text,
            onValueChange = onTextChange,
            label = {
                Text("搜索主题")
            }
        )

        Text("当前输入：${uiState.text}")

        Button(
            onClick = onStartClick,
            enabled = canStart
        ) {
            Text(
                if (uiState.isStarted) {
                    "已开始"
                } else {
                    "开始学习"
                }
            )
        }

        Text(
            if (uiState.isStarted) {
                "当前状态：学习中"
            } else {
                "当前状态：未开始"
            }
        )

        Button(
            onClick = onLoadingClick
        ) {
            Text("切换加载状态")
        }

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Text("加载完成")
        }

        Button(
            onClick = {
                showDetails = !showDetails
            }
        ) {
            Text(
                if (showDetails) {
                    "收起说明"
                } else {
                    "展开说明"
                }
            )
        }

        if (showDetails) {
            Text("这里是 Stage 3 的学习说明")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Stage3ContentPreview() {
    Learn_android_from_AITheme {
        Stage3Content(
            uiState = Stage3UiState(
                text = "Android",
                isStarted = true,
                isLoading = true
            ),
            onTextChange = {},
            onStartClick = {},
            onLoadingClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Stage3ContentNotStartedPreview() {
    Learn_android_from_AITheme {
        Stage3Content(
            uiState = Stage3UiState(),
            onTextChange = {},
            onStartClick = {},
            onLoadingClick = {}
        )
    }
}