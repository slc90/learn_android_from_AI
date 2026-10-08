package com.example.learnandroidfromai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import com.example.learnandroidfromai.ui.theme.Learn_android_from_AITheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.example.learnandroidfromai.ui.todo.TodoScreen
import com.example.learnandroidfromai.ui.todo.TodoViewModel
import com.example.learnandroidfromai.ui.todo.TodoViewModelFactory
import com.example.learnandroidfromai.model.Person
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import android.provider.Settings
import android.annotation.SuppressLint
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.delay
import com.example.learnandroidfromai.ui.bluetooth.BluetoothScreen

class MainActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    private fun tryOpenCamera() {
        try {
            val cameraManager =
                getSystemService(CameraManager::class.java)

            val cameraId =
                cameraManager.cameraIdList.first()

            cameraManager.openCamera(
                cameraId,
                object : CameraDevice.StateCallback() {

                    override fun onOpened(camera: CameraDevice) {
                        println("camera opened successfully")
                        camera.close()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        println("camera disconnected")
                        camera.close()
                    }

                    override fun onError(
                        camera: CameraDevice,
                        error: Int
                    ) {
                        println("camera open error = $error")
                        camera.close()
                    }
                },
                null
            )
        } catch (e: Exception) {
            println(
                "camera open exception = " +
                        "${e::class.simpleName}: ${e.message}"
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val cameraPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                println("camera permission granted = $granted")
            }

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
                var showCameraRationale by remember {
                    mutableStateOf(false)
                }

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
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        BluetoothScreen()
//                        Button(
//                            modifier = Modifier.padding(innerPadding),
//                            onClick = {
//                                println("start compute")
//                                lifecycleScope.launch {
//                                    val result = withContext(Dispatchers.Default) {
//                                        nativeBridge.runMatrixMultiply(1000)
//                                    }
//
//                                    println("matrix result = $result")
//                                    println("end compute")
//                                }
//                            }
//                        ) {
//                            Text("Run Native Compute")
//                        }
//
//                        Button(
//                            onClick = {
//                                val permissionStatus =
//                                    ContextCompat.checkSelfPermission(
//                                        this@MainActivity,
//                                        Manifest.permission.CAMERA
//                                    )
//
//                                if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
//                                    println("camera permission already granted")
//                                } else {
//                                    val shouldShowRationale =
//                                        ActivityCompat.shouldShowRequestPermissionRationale(
//                                            this@MainActivity,
//                                            Manifest.permission.CAMERA
//                                        )
//
//                                    println("shouldShowRationale = $shouldShowRationale")
//
//                                    if (shouldShowRationale) {
//                                        showCameraRationale = true
//                                    } else {
//                                        cameraPermissionLauncher.launch(
//                                            Manifest.permission.CAMERA
//                                        )
//                                    }
//                                }
//                            }
//                        ) {
//                            Text("Request Camera Permission")
//                        }
//
//                        Button(
//                            onClick = {
//                                if (Settings.canDrawOverlays(this@MainActivity)) {
//                                    println("overlay permission already granted")
//                                } else {
//                                    println("overlay permission not granted")
//
//                                    val intent =
//                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
//
//                                    startActivity(intent)
//                                }
//                            }
//                        ) {
//                            Text("Request Overlay Permission")
//                        }
//
//                        Button(
//                            onClick = {
//                                val permissionStatus =
//                                    ContextCompat.checkSelfPermission(
//                                        this@MainActivity,
//                                        Manifest.permission.CAMERA
//                                    )
//
//                                println(
//                                    "camera permission granted = " +
//                                            (permissionStatus == PackageManager.PERMISSION_GRANTED)
//                                )
//
//                                lifecycleScope.launch {
//                                    delay(10_000)
//
//                                    println("trying to open camera...")
//                                    tryOpenCamera()
//                                }
//                            }
//                        ) {
//                            Text("Test Camera Restriction")
//                        }

//                    Stage3App(
//                        modifier = Modifier.padding(innerPadding)
//                    )

//                        val todoViewModel: TodoViewModel = viewModel(
//                            factory = TodoViewModelFactory(
//                                appContainer.todoRepository
//                            )
//                        )
////
//                        val todoUiState by todoViewModel.uiState.collectAsState()
//                        TodoScreen(
//                            uiState = todoUiState,
//                            onLoadTodo = todoViewModel::loadTodo,
//                            onCreateTodo = todoViewModel::createTodo,
//                            onLoadTodosByUser = todoViewModel::loadTodosByUser,
//                            modifier = Modifier.padding(innerPadding)
//                        )
                    }
                    }

                if (showCameraRationale) {
                    AlertDialog(
                        onDismissRequest = {
                            showCameraRationale = false
                        },
                        title = {
                            Text("需要相机权限")
                        },
                        text = {
                            Text("这个功能需要使用相机权限。")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showCameraRationale = false

                                    cameraPermissionLauncher.launch(
                                        Manifest.permission.CAMERA
                                    )
                                }
                            ) {
                                Text("继续申请")
                            }
                        }
                    )
                }
                }


            }
        }
}