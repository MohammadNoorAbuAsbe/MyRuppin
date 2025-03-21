package com.MohammadNoorAbuAsbe.myruppin

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.MohammadNoorAbuAsbe.myruppin.workers.GradeCheckWorker
import java.util.concurrent.TimeUnit

class MyRuppin : Application() {

    override fun onCreate() {
        super.onCreate()

        // Schedule the WorkManager task
        val gradeCheckRequest = PeriodicWorkRequestBuilder<GradeCheckWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "GradeCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            gradeCheckRequest
        )
    }
}