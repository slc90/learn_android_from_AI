package com.example.learnandroidfromai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.learnandroidfromai.ui.theme.Learn_android_from_AITheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
//                    LearningList(
//                        modifier = Modifier.padding(innerPadding)
//                    )
                    Stage3Screen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


}

@Composable
fun Stage3Screen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var text by remember {
            mutableStateOf("")
        }

        Column {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                },
                label = {
                    Text("搜索主题")
                }
            )

            Text("当前输入：$text")
        }

        Text(
            text = "Modifier Test",
            modifier = Modifier
                .background(Color.LightGray)
                .padding(24.dp)
        )

        Text(
            text = "Modifier Test",
            modifier = Modifier
                .padding(24.dp)
                .background(Color.LightGray)
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text("Hello Box")
        }

        Text("Stage 3")

        Text("Jetpack Compose")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier.weight(2f)
            ) {
                Text("开始学习")
            }

            Button(
                onClick = {},
                modifier = Modifier.weight(1f)
            ) {
                Text("查看进度")
            }
        }

        LearningCard()
    }
}

@Composable
fun LearningCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Stage 3",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Jetpack Compose 界面开发",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {}
            ) {
                Text("继续学习")
            }
        }
    }
}

@Composable
fun LearningList(modifier: Modifier = Modifier) {
    val topics = listOf(
        Topic("Compose 布局", "Column、Row、Box"),
        Topic("Material 组件", "Card、Button、TopAppBar"),
        Topic("界面状态", "后面会学")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(topics) { topic ->
            TopicItem(
                topic = topic,
                onClick = {
                    println("点击了：${topic.title}")
                }
            )
        }
    }
}

data class Topic(
    val title: String,
    val description: String
)

@Composable
fun TopicItem(
    topic: Topic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = topic.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}