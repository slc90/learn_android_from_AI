package com.example.learnandroidfromai.ui.bluetooth

import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material3.Button
import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.bluetooth.BluetoothProfile

data class ScannedBleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val serviceUuids: String
)

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen() {
    val context = LocalContext.current

    val packageManager = context.packageManager

    val bluetoothSupported =
        packageManager.hasSystemFeature(
            PackageManager.FEATURE_BLUETOOTH
        )

    val bleSupported =
        packageManager.hasSystemFeature(
            PackageManager.FEATURE_BLUETOOTH_LE
        )

    val bluetoothManager =
        context.getSystemService(BluetoothManager::class.java)

    val bluetoothAdapter =
        bluetoothManager.adapter

    var bluetoothEnabled by remember {
        mutableStateOf(
            bluetoothAdapter?.isEnabled == true
        )
    }

    val scanPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

    var scanPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                scanPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val scanPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            scanPermissionGranted = granted
            println("Bluetooth scan permission granted = $granted")
        }

    var scanning by remember {
        mutableStateOf(false)
    }

    var scanCount by remember {
        mutableStateOf(0)
    }

    var lastDeviceName by remember {
        mutableStateOf<String?>(null)
    }

    var lastRssi by remember {
        mutableStateOf<Int?>(null)
    }

    val uniqueDevices = remember {
        mutableStateMapOf<String, ScannedBleDevice>()
    }

    var pairedNames by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var pairedMessage by remember {
        mutableStateOf("尚未读取")
    }

    var a2dpNames by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var a2dpMessage by remember {
        mutableStateOf("尚未查询 A2DP 连接")
    }

    val a2dpListener = remember(bluetoothAdapter) {
        object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile
            ) {
                try {
                    if (profile == BluetoothProfile.A2DP) {
                        a2dpNames = proxy.connectedDevices.map {
                            it.name ?: "(未命名设备)"
                        }

                        a2dpMessage =
                            "A2DP 已连接设备：${a2dpNames.size} 个"
                    }
                } catch (e: SecurityException) {
                    a2dpMessage = "缺少蓝牙连接权限"
                } finally {
                    bluetoothAdapter?.closeProfileProxy(
                        profile,
                        proxy
                    )
                }
            }

            override fun onServiceDisconnected(profile: Int) {
            }
        }
    }

    val scanCallback = remember {
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                scanCount++

                val address = result.device.address
                val scanRecord = result.scanRecord

                val name = scanRecord?.deviceName

                val serviceUuids =
                    scanRecord?.serviceUuids
                        ?.joinToString()
                        ?: "-"

                uniqueDevices[address] =
                    ScannedBleDevice(
                        address = address,
                        name = name,
                        rssi = result.rssi,
                        serviceUuids = serviceUuids
                    )
            }

            override fun onScanFailed(errorCode: Int) {
                scanning = false

                println(
                    "BLE scan failed: $errorCode"
                )
            }
        }
    }

    fun readPairedDevices() {
        if (!bluetoothEnabled) {
            pairedMessage = "请先打开蓝牙"
            return
        }

        try {
            pairedNames = bluetoothAdapter
                ?.bondedDevices
                ?.map { device ->
                    device.name ?: "(未命名设备)"
                }
                ?.sorted()
                ?: emptyList()

            pairedMessage = "已配对设备：${pairedNames.size} 个"

        } catch (e: SecurityException) {
            pairedMessage = "缺少蓝牙连接权限"
        }
    }

    val connectPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                readPairedDevices()
            } else {
                pairedMessage = "用户拒绝了蓝牙连接权限"
            }
        }

    LaunchedEffect(scanning) {
        if (scanning) {
            delay(10_000)

            bluetoothAdapter
                ?.bluetoothLeScanner
                ?.stopScan(scanCallback)

            scanning = false

            println("BLE scan stopped")
        }
    }

    DisposableEffect(context) {

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                    val state =
                        intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )

                    bluetoothEnabled =
                        state == BluetoothAdapter.STATE_ON

                    println("Bluetooth state changed: $state")
                }
            }
        }

        val filter =
            IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED
            )

        context.registerReceiver(
            receiver,
            filter
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column {
        Text("Bluetooth: $bluetoothSupported")
        Text("BLE: $bleSupported")
        Text("BluetoothAdapter: ${bluetoothAdapter != null}")
        Text("Bluetooth enabled: $bluetoothEnabled")
        Text(
            "Scan permission: $scanPermissionGranted"
        )

        Button(
            onClick = {
                scanPermissionLauncher.launch(
                    scanPermission
                )
            }
        ) {
            Text("Request Bluetooth Scan Permission")
        }

        Button(
            enabled =
                scanPermissionGranted &&
                        bluetoothEnabled,
            onClick = {
                val scanner =
                    bluetoothAdapter?.bluetoothLeScanner

                if (scanner == null) {
                    println("BLE scanner unavailable")
                    return@Button
                }

                if (!scanning) {
                    scanCount = 0
                    uniqueDevices.clear()

                    lastDeviceName = null
                    lastRssi = null

                    scanner.startScan(scanCallback)

                    scanning = true
                } else {
                    scanner.stopScan(scanCallback)

                    scanning = false
                    println("BLE scan stopped")
                }
            }
        ) {
            Text(
                if (scanning) {
                    "Stop BLE Scan"
                } else {
                    "Start BLE Scan"
                }
            )
        }
        Text("Scanning: $scanning")
        Text("Scan results: $scanCount")
        Text("Unique devices: ${uniqueDevices.size}")

        Button(
            onClick = {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    connectPermissionLauncher.launch(
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                } else {
                    readPairedDevices()
                }
            }
        ) {
            Text("查看已配对设备")
        }

        Text(pairedMessage)

        pairedNames.forEach { name ->
            Text(name)
        }

        Button(
            onClick = {
                if (!bluetoothEnabled) {
                    a2dpMessage = "蓝牙未开启"
                } else if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    a2dpMessage = "请先申请蓝牙连接权限"
                } else {
                    a2dpMessage = "正在查询..."

                    try {
                        val started = bluetoothAdapter?.getProfileProxy(
                            context,
                            a2dpListener,
                            BluetoothProfile.A2DP
                        ) ?: false

                        if (!started) {
                            a2dpMessage = "无法获取 A2DP 服务"
                        }
                    } catch (e: SecurityException) {
                        a2dpMessage = "缺少蓝牙连接权限"
                    }
                }
            }
        ) {
            Text("查看 A2DP 连接")
        }

        Text(a2dpMessage)

        a2dpNames.forEach { name ->
            Text("已连接：$name")
        }

        LazyColumn(
            modifier = Modifier.height(350.dp)
        ) {
            items(
                uniqueDevices.values
                    .sortedByDescending { it.rssi }
            ) { device ->

                Text(
                    """
            Name: ${device.name ?: "(no name)"}
            Address: ${device.address}
            RSSI: ${device.rssi}
            UUID: ${device.serviceUuids}
            """.trimIndent()
                )
            }
        }
        Text("Last device: ${lastDeviceName ?: "-"}")
        Text("RSSI: ${lastRssi ?: "-"}")
    }
}