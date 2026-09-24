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
import java.io.File

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

        runStorageExperiment()

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

    private fun runStorageExperiment() {
        // 1. Internal 持久文件
        val internalFile = File(filesDir, "internal_test.txt")
        internalFile.writeText("Hello from filesDir")

        // 2. Internal Cache
        val cacheFile = File(cacheDir, "cache_test.txt")
        cacheFile.writeText("Hello from cacheDir")

        // 3. External App-specific 持久文件
        val externalDir = getExternalFilesDir(null)
        val externalFile = externalDir?.let {
            File(it, "external_test.txt").apply {
                writeText("Hello from external files dir")
            }
        }

        Log.d("StorageTest", "filesDir = ${internalFile.absolutePath}")
        Log.d("StorageTest", "cacheDir = ${cacheFile.absolutePath}")
        Log.d("StorageTest", "external = ${externalFile?.absolutePath}")
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