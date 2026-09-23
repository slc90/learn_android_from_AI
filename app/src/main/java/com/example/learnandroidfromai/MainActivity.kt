package com.example.learnandroidfromai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.learnandroidfromai.ui.theme.Learn_android_from_AITheme
import android.util.Log
import android.content.Intent
import androidx.compose.material3.Button
import android.os.Process

class MainActivity : ComponentActivity() {
    private var counter = 0

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("counter", counter)
        logLifecycle("onSaveInstanceState counter = $counter")

        super.onSaveInstanceState(outState)
    }

    private val instanceId = java.util.UUID.randomUUID().toString()

    private fun logLifecycle(message: String) {
        Log.d(
            "LifecycleTest",
            "pid=${Process.myPid()} MainActivity [$instanceId] $message"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        counter = savedInstanceState?.getInt("counter") ?: 0

        logLifecycle("onCreate, counter = $counter")

        enableEdgeToEdge()
        setContent {
            Learn_android_from_AITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Button(
                        onClick = {
                            startActivity(
                                Intent(this@MainActivity, SecondActivity::class.java)
                            )
                        },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Text("Open SecondActivity")
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        logLifecycle("onStart")
    }

    override fun onResume() {
        super.onResume()
        logLifecycle("onResume")
    }

    override fun onPause() {
        super.onPause()
        logLifecycle("onPause")
    }

    override fun onStop() {
        super.onStop()
        logLifecycle("onStop")
    }

    override fun onRestart() {
        super.onRestart()
        logLifecycle("onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        logLifecycle("onDestroy")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Learn_android_from_AITheme {
        Greeting("Android")
    }
}