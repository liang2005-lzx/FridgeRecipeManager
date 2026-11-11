package com.liang.fridgerecipe.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.ActivityMainBinding
import com.liang.fridgerecipe.util.PreferenceStore
import com.liang.fridgerecipe.work.NotificationHelper
import com.liang.fridgerecipe.work.ReminderScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // 用户拒绝也不影响主流程，只是不发通知（页面上有一致性提示）
            if (granted) {
                val prefs = PreferenceStore(this)
                if (prefs.notificationsEnabled) {
                    ReminderScheduler.schedule(this, prefs.reminderHour, prefs.reminderMinute)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_container) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)

        maybeAskNotificationPermission()
    }

    private fun maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = PreferenceStore(this)
        if (prefs.askedNotificationPermission) return
        if (NotificationHelper.canPostNotification(this)) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return

        prefs.askedNotificationPermission = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
