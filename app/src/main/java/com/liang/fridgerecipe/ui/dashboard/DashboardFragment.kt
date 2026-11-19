package com.liang.fridgerecipe.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.FragmentDashboardBinding
import com.liang.fridgerecipe.ui.ingredient.IngredientAdapter
import kotlinx.coroutines.launch

/**
 * 界面 6：数据看板（库存统计 + 自定义柱状图 + 优先处理清单）。
 */
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var priorityAdapter: IngredientAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDashboardBinding.bind(view)

        priorityAdapter = IngredientAdapter { item ->
            findNavController().navigate(
                R.id.ingredientDetailFragment,
                bundleOf("ingredientId" to item.id)
            )
        }
        binding.rvPriority.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPriority.adapter = priorityAdapter

        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        when (state) {
            is DashboardUiState.Loading -> {
                binding.tvSubtitle.text = "统计中…"
            }

            is DashboardUiState.Error -> {
                binding.tvSubtitle.text = state.message
            }

            is DashboardUiState.Content -> {
                val stats = state.stats
                binding.tvSubtitle.text = "一共 ${stats.total} 样食材 · ${stats.byCategory.size} 个分类"
                binding.tvTotal.text = stats.total.toString()
                binding.tvUrgent.text = stats.warningCount.toString()
                binding.tvWaste.text = "${stats.wasteRate}%"
                binding.chartCategory.setData(stats.byCategory)

                priorityAdapter.submitList(state.priority)
                val empty = state.priority.isEmpty()
                binding.tvNoUrgent.isVisible = empty
                binding.rvPriority.isVisible = !empty
            }
        }
    }

    override fun onDestroyView() {
        binding.rvPriority.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
