package com.example.learnandroidfromai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat

class ForegroundServiceTest : Service() {

    private var running = false
    private var workerThread: Thread? = null

    override fun onCreate() {
        super.onCreate()

        Log.d(
            "ForegroundServiceTest",
            "onCreate, pid=${Process.myPid()}"
        )

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(
            "ForegroundServiceTest",
            "onStartCommand, pid=${Process.myPid()}"
        )

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service Test")
                .setContentText("模拟数据同步正在运行")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }

        if (!running) {
            running = true

            workerThread = Thread {
                var count = 0

                while (running) {
                    Log.d(
                        "ForegroundServiceTest",
                        "working... count=$count, pid=${Process.myPid()}, thread=${Thread.currentThread().name}"
                    )

                    count++

                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }.apply {
                name = "FgsExperimentThread"
                start()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(
            "ForegroundServiceTest",
            "onDestroy, pid=${Process.myPid()}"
        )

        running = false
        workerThread?.interrupt()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Test",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "foreground_service_test"
        private const val NOTIFICATION_ID = 1001
    }
}