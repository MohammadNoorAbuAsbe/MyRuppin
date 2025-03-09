package com.MohammadNoorAbuAsbe.myruppin

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.MohammadNoorAbuAsbe.myruppin.screens.GradesScreen
import com.MohammadNoorAbuAsbe.myruppin.screens.HomeScreen
import com.MohammadNoorAbuAsbe.myruppin.screens.LoginScreen
import com.MohammadNoorAbuAsbe.myruppin.ui.theme.MyRuppinTheme
import android.Manifest
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.MohammadNoorAbuAsbe.myruppin.screens.ScheduleScreen
import com.MohammadNoorAbuAsbe.myruppin.workers.GradeCheckWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
            }
        }

        // Schedule the GradeCheckWorker to run periodically
        val workRequest = PeriodicWorkRequestBuilder<GradeCheckWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "GradeCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )


        setContent {
            MyRuppinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination =  "login"
    ) {
        composable("login") {
            LoginScreen(navController)
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("grades") {
            GradesScreen(navController)
        }
        composable("schedule") {
            ScheduleScreen(navController)
        }
    }
}