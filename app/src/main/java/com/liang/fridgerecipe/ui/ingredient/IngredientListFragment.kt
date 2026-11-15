package com.liang.fridgerecipe.ui.ingredient

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.FragmentIngredientListBinding
import com.liang.fridgerecipe.util.PreferenceStore
import com.liang.fridgerecipe.work.NotificationHelper
import com.liang.fridgerecipe.work.ReminderScheduler
import kotlinx.coroutines.launch

/**
 * 界面 1：食材列表（首页）。支持搜索 + 状态筛选 + 空态/加载态/错误态。
 */
class IngredientListFragment : Fragment(R.layout.fragment_ingredient_list) {

    private var _binding: FragmentIngredientListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IngredientListViewModel by viewModels()
    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var prefs: PreferenceStore

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            updatePermissionBanner()
            if (granted) {
                ReminderScheduler.schedule(
                    requireContext(), prefs.reminderHour, prefs.reminderMinute
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIngredientListBinding.bind(view)
        prefs = PreferenceStore(requireContext())

        setupRecyclerView()
        setupSearch()
        setupChips()
        setupFab()
        binding.tvPermissionBanner.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        binding.btnRetry.setOnClickListener { viewModel.retry() }

        collectState()
    }

    private fun setupRecyclerView() {
        ingredientAdapter = IngredientAdapter { item ->
            findNavController().navigate(
                R.id.ingredientDetailFragment,
                bundleOf("ingredientId" to item.id)
            )
        }
        binding.rvIngredients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIngredients.adapter = ingredientAdapter
    }

    private fun setupSearch() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.setKeyword(text?.toString().orEmpty())
        }
    }

    private fun setupChips() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_expiring -> IngredientFilter.EXPIRING
                R.id.chip_expired -> IngredientFilter.EXPIRED
                R.id.chip_fresh -> IngredientFilter.FRESH
                else -> IngredientFilter.ALL
            }
            viewModel.setFilter(filter)
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(
                R.id.ingredientEditFragment,
                bundleOf("ingredientId" to -1L)
            )
        }
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionBanner()
    }

    private fun updatePermissionBanner() {
        val needBanner = prefs.notificationsEnabled && !NotificationHelper.canPostNotification(requireContext())
        binding.tvPermissionBanner.isVisible = needBanner
    }

    private fun render(state: IngredientListUiState) {
        binding.progressLoading.isVisible = state is IngredientListUiState.Loading
        binding.errorContainer.isVisible = state is IngredientListUiState.Error
        binding.rvIngredients.isVisible = state is IngredientListUiState.Content

        when (state) {
            is IngredientListUiState.Loading -> {
                binding.emptyState.isVisible = false
            }

            is IngredientListUiState.Error -> {
                binding.emptyState.isVisible = false
                binding.tvError.text = state.message
            }

            is IngredientListUiState.Content -> {
                binding.tvSubtitle.text = "一共 ${state.totalCount} 样食材"

                val hasWarning = state.warningCount > 0
                binding.tvWarning.isVisible = hasWarning
                binding.tvWarning.text = "${state.warningCount} 个待处理"

                val empty = state.items.isEmpty()
                binding.emptyState.isVisible = empty
                if (empty) {
                    val filtering = state.keyword.isNotBlank() || state.filter != IngredientFilter.ALL
                    binding.tvEmptyEmoji.text = if (filtering) "🔍" else "🧺"
                    binding.tvEmptyTitle.text = if (filtering) "没有符合条件的食材" else "还没有食材"
                    binding.tvEmptySubtitle.text =
                        if (filtering) "换个关键词或筛选条件试试" else "点击右下角按钮，把冰箱里的东西记下来"
                }
                ingredientAdapter.submitList(state.items)
            }
        }
    }

    override fun onDestroyView() {
        binding.rvIngredients.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
