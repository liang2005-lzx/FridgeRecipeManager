package com.liang.fridgerecipe.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
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
import com.liang.fridgerecipe.databinding.DialogAiRecipeBinding
import com.liang.fridgerecipe.databinding.FragmentRecipeListBinding
import com.liang.fridgerecipe.model.Recipe
import kotlinx.coroutines.launch

/**
 * 界面 4：菜谱列表（按冰箱匹配度排序，可切换"只看能做的"）。
 * 联网入口：☁ 同步云端菜谱、✨ AI 生成一道菜，失败都降级为本地数据。
 */
class RecipeListFragment : Fragment(R.layout.fragment_recipe_list) {

    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeListViewModel by viewModels()
    private val cloudViewModel: CloudRecipeViewModel by viewModels()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRecipeListBinding.bind(view)

        recipeAdapter = RecipeAdapter { match ->
            findNavController().navigate(
                R.id.recipeDetailFragment,
                bundleOf("recipeId" to match.recipe.id, "ingredientId" to -1L)
            )
        }
        binding.rvRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecipes.adapter = recipeAdapter

        binding.switchCookable.setOnCheckedChangeListener { _, checked ->
            viewModel.setOnlyCookable(checked)
        }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
        binding.btnCloudSync.setOnClickListener { cloudViewModel.syncFromCloud() }
        binding.btnAiGenerate.setOnClickListener { cloudViewModel.generateByAi() }

        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> render(state) } }
                launch { cloudViewModel.syncState.collect { state -> renderSync(state) } }
                launch { cloudViewModel.aiState.collect { state -> renderAi(state) } }
            }
        }
    }

    private fun render(state: RecipeListUiState) {
        binding.progressLoading.isVisible = state is RecipeListUiState.Loading
        binding.errorContainer.isVisible = state is RecipeListUiState.Error
        binding.rvRecipes.isVisible = state is RecipeListUiState.Content

        when (state) {
            is RecipeListUiState.Loading -> binding.emptyState.isVisible = false

            is RecipeListUiState.Error -> {
                binding.emptyState.isVisible = false
                binding.tvError.text = state.message
            }

            is RecipeListUiState.Content -> {
                binding.tvSubtitle.text =
                    "共 ${state.totalCount} 道菜谱 · 现在能直接做 ${state.cookableCount} 道"
                recipeAdapter.submitList(state.matches)

                val empty = state.matches.isEmpty()
                binding.emptyState.isVisible = empty
                if (empty) {
                    binding.tvEmptyTitle.text =
                        if (state.onlyCookable) "没有能直接做的菜" else "还没有菜谱数据"
                    binding.tvEmptySubtitle.text =
                        if (state.onlyCookable) "关掉开关看看差一点的菜谱，或者先添加食材"
                        else "点上方「同步云端」拉取菜谱，或到设置页恢复内置菜谱"
                }
            }
        }
    }

    private fun renderSync(state: SyncState) {
        when (state) {
            is SyncState.Idle -> {
                binding.tvCloudStatus.isVisible = false
                binding.btnCloudSync.isEnabled = true
                binding.btnCloudSync.text = "☁ 同步云端"
            }

            is SyncState.Loading -> {
                binding.tvCloudStatus.isVisible = true
                binding.tvCloudStatus.text = "正在从云端拉取菜谱…"
                binding.btnCloudSync.isEnabled = false
                binding.btnCloudSync.text = "同步中…"
            }

            is SyncState.Success -> {
                binding.tvCloudStatus.isVisible = true
                binding.tvCloudStatus.text = "已同步 ${state.count} 道云端菜谱（网络正常）"
                binding.btnCloudSync.isEnabled = true
                binding.btnCloudSync.text = "☁ 同步云端"
            }

            is SyncState.Error -> {
                // 网络失败不阻塞使用：继续展示本地菜谱，只提示降级原因
                binding.tvCloudStatus.isVisible = true
                binding.tvCloudStatus.text = "离线模式：${state.message}"
                binding.btnCloudSync.isEnabled = true
                binding.btnCloudSync.text = "☁ 重试同步"
            }
        }
    }

    private fun renderAi(state: AiState) {
        when (state) {
            is AiState.Idle -> {
                binding.btnAiGenerate.isEnabled = true
                binding.btnAiGenerate.text = "✨ AI 生成一道菜"
            }

            is AiState.Loading -> {
                binding.btnAiGenerate.isEnabled = false
                binding.btnAiGenerate.text = "AI 思考中…"
            }

            is AiState.Result -> {
                binding.btnAiGenerate.isEnabled = true
                binding.btnAiGenerate.text = "✨ AI 生成一道菜"
                showAiDialog(state.recipe)
                cloudViewModel.resetAiState()
            }

            is AiState.Error -> {
                binding.btnAiGenerate.isEnabled = true
                binding.btnAiGenerate.text = "✨ AI 生成一道菜"
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                cloudViewModel.resetAiState()
            }
        }
    }

    private fun showAiDialog(recipe: Recipe) {
        val dialogBinding = DialogAiRecipeBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.tvAiTitle.text = "${recipe.emoji} ${recipe.name}"
        dialogBinding.tvAiMeta.text = "${recipe.minutesText} · ${recipe.difficultyText}"
        dialogBinding.tvAiIngredients.text = recipe.ingredients.joinToString("\n") {
            "· ${it.name} ${it.amount}" + if (it.optional) "（可选）" else ""
        }
        dialogBinding.tvAiSteps.text = recipe.steps.mapIndexed { index, step ->
            "${index + 1}. $step"
        }.joinToString("\n")
        dialogBinding.tvAiTips.text = "小贴士：${recipe.tips.ifBlank { "无" }}"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI 生成的菜谱")
            .setView(dialogBinding.root)
            .setNegativeButton("关闭", null)
            .setPositiveButton("保存到我的菜谱") { _, _ ->
                cloudViewModel.saveAiRecipe(recipe)
                Toast.makeText(requireContext(), "已保存「${recipe.name}」", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        binding.rvRecipes.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
