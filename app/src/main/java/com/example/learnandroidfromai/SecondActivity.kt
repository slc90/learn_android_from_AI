package com.example.learnandroidfromai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


class SecondActivity : ComponentActivity() {

    override fun onSaveInstanceState(outState: Bundle) {
        logLifecycle("onSaveInstanceState")
        super.onSaveInstanceState(outState)
    }

    private fun logLifecycle(message: String) {
        Log.d("LifecycleTest", "SecondActivity $message")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logLifecycle("onCreate")

        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface {
                    Text(
                        text = "SecondActivity",
                        modifier = Modifier.padding(32.dp)
                    )
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