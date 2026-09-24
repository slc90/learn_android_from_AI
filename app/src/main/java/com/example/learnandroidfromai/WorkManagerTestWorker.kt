package com.example.learnandroidfromai

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class WorkManagerTestWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d(
            "WorkManagerTest",
            "Worker executed, pid=${Process.myPid()}, attempt=$runAttemptCount"
        )

        return Result.success()
    }
}