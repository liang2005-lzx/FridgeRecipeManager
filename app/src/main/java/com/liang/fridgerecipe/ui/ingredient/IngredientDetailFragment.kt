package com.liang.fridgerecipe.ui.ingredient

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.FragmentIngredientDetailBinding
import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.domain.ExpiryStatus
import com.liang.fridgerecipe.ui.common.StatusColors
import com.liang.fridgerecipe.util.DateUtils
import kotlinx.coroutines.launch

/**
 * 界面 2：食材详情。列表 → 详情的导航结构在这里体现。
 */
class IngredientDetailFragment : Fragment(R.layout.fragment_ingredient_detail) {

    private var _binding: FragmentIngredientDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IngredientDetailViewModel by viewModels()
    private lateinit var miniAdapter: RecipeMiniAdapter
    private var closeGuard = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIngredientDetailBinding.bind(view)

        miniAdapter = RecipeMiniAdapter { match ->
            findNavController().navigate(
                R.id.recipeDetailFragment,
                bundleOf("recipeId" to match.recipe.id, "ingredientId" to -1L)
            )
        }
        binding.rvRecommendations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommendations.adapter = miniAdapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnEdit.setOnClickListener {
            findNavController().navigate(
                R.id.ingredientEditFragment,
                bundleOf("ingredientId" to (viewModel.uiState.value.ingredient?.id ?: -1L))
            )
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> render(state) }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            IngredientDetailEvent.Deleted -> {
                                closeGuard = true
                                findNavController().navigateUp()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun confirmDelete() {
        val item = viewModel.uiState.value.ingredient ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除「${item.name}」？")
            .setMessage("删除后无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> viewModel.deleteIngredient() }
            .show()
    }

    private fun render(state: IngredientDetailUiState) {
        val item = state.ingredient
        if (item == null) {
            // 被删除后数据流会先变空，这里只回退一次，避免重复导航
            if (!closeGuard && !state.loading) {
                closeGuard = true
                findNavController().navigateUp()
            }
            return
        }

        val context = requireContext()
        val info = ExpiryAnalyzer.info(item)
        val color = StatusColors.textColor(context, info.status)
        val bg = StatusColors.backgroundColor(context, info.status)

        binding.tvEmoji.text = item.category.emoji
        binding.tvName.text = item.name
        binding.tvCategory.text = item.category.label
        binding.tvStatus.text = info.status.label
        binding.tvStatus.setTextColor(color)
        binding.tvStatus.backgroundTintList = ColorStateList.valueOf(bg)

        binding.tvQuantity.text = "${item.quantityText} ${item.unit}"
        binding.tvAdded.text = DateUtils.toDayText(item.addedAt)
        binding.tvExpires.text = DateUtils.toDayText(item.expiresAt)

        binding.tvRiskLabel.text = if (info.status == ExpiryStatus.EXPIRED) {
            "已经过期 ${-info.daysLeft} 天，尽快处理"
        } else {
            "新鲜度：${DateUtils.daysLeftText(info.daysLeft)}"
        }
        binding.progressRisk.progress = info.risk
        binding.progressRisk.progressTintList = ColorStateList.valueOf(color)
        binding.tvRiskHint.text = "风险 ${info.risk}/100 · 参考保质期 ${item.category.defaultShelfLifeDays} 天"

        val note = item.note.trim()
        binding.tvNote.isVisible = note.isNotEmpty()
        binding.tvNote.text = "备注：$note"

        miniAdapter.submitList(state.recommendations)
        val empty = state.recommendations.isEmpty()
        binding.tvNoRecipe.isVisible = empty
        binding.tvRecipeHint.isVisible = !empty
    }

    override fun onDestroyView() {
        binding.rvRecommendations.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
