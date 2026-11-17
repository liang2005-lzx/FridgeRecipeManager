package com.liang.fridgerecipe.ui.recipe

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.FragmentRecipeDetailBinding
import kotlinx.coroutines.launch

/**
 * 界面 5：菜谱详情。显示做法步骤，并逐样标注"有 / 临期 / 缺 / 过期"。
 */
class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeDetailViewModel by viewModels()
    private var leaveGuard = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRecipeDetailBinding.bind(view)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: RecipeDetailUiState) {
        val recipe = state.recipe
        if (recipe == null) {
            if (!state.loading && !leaveGuard) {
                leaveGuard = true
                findNavController().navigateUp()
            }
            return
        }

        val context = requireContext()
        binding.tvEmoji.text = recipe.emoji
        binding.tvName.text = recipe.name
        binding.tvMeta.text = "${recipe.category} · ${recipe.minutesText} · ${recipe.difficultyText}"

        val match = state.match
        binding.tvScore.text = "${match?.score ?: 0} 分"
        val canCook = match?.canCook == true
        val color = if (canCook) {
            context.getColor(R.color.green_primary)
        } else {
            context.getColor(R.color.orange_urgent)
        }
        binding.tvScore.setTextColor(color)
        binding.tvScore.backgroundTintList = ColorStateList.valueOf(
            if (canCook) context.getColor(R.color.green_light) else context.getColor(R.color.orange_light)
        )
        binding.tvMatchReason.text = match?.reason ?: "还没有冰箱数据，先添加食材看匹配度"

        renderIngredients(state.lines)

        val stepsContainer = binding.containerSteps
        stepsContainer.removeAllViews()
        recipe.steps.forEachIndexed { index, step ->
            val row = LayoutInflater.from(context)
                .inflate(R.layout.item_step, stepsContainer, false)
            row.findViewById<TextView>(R.id.tv_step_index).text = (index + 1).toString()
            row.findViewById<TextView>(R.id.tv_step_text).text = step
            stepsContainer.addView(row)
        }

        binding.tvTips.text = recipe.tips.ifBlank { "这道菜暂时没有额外提示" }
        binding.cardTips.isVisible = recipe.tips.isNotBlank()
    }

    private fun renderIngredients(lines: List<IngredientStockLine>) {
        val context = requireContext()
        val container = binding.containerIngredients
        container.removeAllViews()

        lines.forEach { line ->
            val row = LayoutInflater.from(context)
                .inflate(R.layout.item_recipe_ingredient_line, container, false)

            val mark = row.findViewById<TextView>(R.id.tv_mark)
            val name = row.findViewById<TextView>(R.id.tv_ingredient_name)
            val amount = row.findViewById<TextView>(R.id.tv_ingredient_amount)
            val tag = row.findViewById<TextView>(R.id.tv_ingredient_tag)

            name.text = line.ingredient.name
            amount.text = line.ingredient.amount

            val (markText, tagText, tagColor) = when (line.state) {
                StockState.AVAILABLE -> Triple("✓", "有", context.getColor(R.color.green_primary))
                StockState.EXPIRING -> Triple("!", "临期", context.getColor(R.color.orange_urgent))
                StockState.EXPIRED -> Triple("×", "已过期", context.getColor(R.color.red_expired))
                StockState.MISSING -> Triple("·", if (line.ingredient.optional) "可选" else "缺", context.getColor(R.color.text_secondary))
            }

            mark.text = markText
            mark.setTextColor(tagColor)
            tag.text = tagText
            tag.setTextColor(tagColor)
            row.alpha = if (line.state == StockState.MISSING) 0.7f else 1f
            container.addView(row)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
