package com.liang.fridgerecipe.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.liang.fridgerecipe.BuildConfig
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.FragmentSettingsBinding
import com.liang.fridgerecipe.util.PreferenceStore
import com.liang.fridgerecipe.work.NotificationHelper
import com.liang.fridgerecipe.work.ReminderScheduler
import androidx.work.WorkManager
import kotlinx.coroutines.launch

/**
 * 界面 7：设置页。提醒开关 / 提醒时间 / 通知权限降级 / 数据管理。
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var prefs: PreferenceStore

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                prefs.notificationsEnabled = true
                ReminderScheduler.schedule(requireContext(), prefs.reminderHour, prefs.reminderMinute)
            } else {
                Toast.makeText(requireContext(), "没有通知权限，提醒只能在 App 内查看", Toast.LENGTH_SHORT).show()
            }
            refreshUi()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsBinding.bind(view)
        prefs = PreferenceStore(requireContext())

        binding.switchNotify.isChecked = prefs.notificationsEnabled
        binding.switchNotify.setOnCheckedChangeListener { _, checked ->
            prefs.notificationsEnabled = checked
            if (checked) {
                ensureNotificationPath()
                ReminderScheduler.schedule(requireContext(), prefs.reminderHour, prefs.reminderMinute)
            } else {
                ReminderScheduler.cancel(requireContext())
            }
            refreshUi()
        }

        binding.rowTime.setOnClickListener { showTimePicker() }
        binding.tvPermission.setOnClickListener { openSystemNotificationSettings() }

        binding.btnDemo.setOnClickListener { viewModel.importDemoIngredients() }
        binding.btnReimport.setOnClickListener { viewModel.restoreRecipes() }
        binding.btnClear.setOnClickListener { confirmClear() }

        binding.tvVersion.text = "冰箱菜谱 v${BuildConfig.VERSION_NAME}"

        collectMessages()
        refreshUi()
    }

    private fun collectMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun ensureNotificationPath() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.canPostNotification(requireContext())
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openSystemNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }
        runCatching { startActivity(intent) }
            .onFailure {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                })
            }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(prefs.reminderHour)
            .setMinute(prefs.reminderMinute)
            .setTitleText("选择每天提醒时间")
            .build()

        picker.addOnPositiveButtonClickListener {
            prefs.reminderHour = picker.hour
            prefs.reminderMinute = picker.minute
            if (prefs.notificationsEnabled) {
                ReminderScheduler.schedule(requireContext(), prefs.reminderHour, prefs.reminderMinute)
            }
            refreshUi()
        }
        picker.show(childFragmentManager, "time_picker")
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空全部数据？")
            .setMessage("会删除所有食材和菜谱，操作不可恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("确认清空") { _, _ -> viewModel.clearAll() }
            .show()
    }

    /** 把权限状态、提醒时间、后台任务状态全部同步到界面上 */
    private fun refreshUi() {
        binding.tvReminderTime.text = prefs.reminderTimeText

        val canNotify = NotificationHelper.canPostNotification(requireContext())
        binding.tvPermission.isVisible = !canNotify
        binding.tvPermission.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "通知权限未开启，点击去系统设置开启"
        } else {
            "系统通知已关闭，点击去设置开启"
        }

        val enabled = prefs.notificationsEnabled
        binding.switchNotify.isChecked = enabled

        // 查询后台任务状态要读 WorkManager 数据库，放到 IO 线程，避免卡主线程
        viewLifecycleOwner.lifecycleScope.launch {
            val status = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                queryWorkState(enabled)
            }
            _binding?.tvWorkState?.text = status
        }
    }

    private fun queryWorkState(enabled: Boolean): String {
        if (!enabled) return "后台任务：已关闭，不会发送提醒"
        return runCatching {
            val workInfos = WorkManager.getInstance(requireContext())
                .getWorkInfosForUniqueWork("expiry_daily_check")
                .get()
            val hasWork = workInfos.any { !it.state.isFinished }
            if (hasWork) {
                "后台任务：已排期，每天 ${prefs.reminderTimeText} 检查一次"
            } else {
                "后台任务：未排期，重新打开开关即可启用"
            }
        }.getOrElse {
            "后台任务：状态读取失败"
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
