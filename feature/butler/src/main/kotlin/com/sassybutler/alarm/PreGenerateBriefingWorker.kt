package com.sassybutler.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import com.sassybutler.alarm.di.ButlerAiProviderEntryPoint

class PreGenerateBriefingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ButlerAiProviderEntryPoint::class.java
            )
            val aiProvider = entryPoint.getButlerAiProvider()
            aiProvider.preGenerateBriefing(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
