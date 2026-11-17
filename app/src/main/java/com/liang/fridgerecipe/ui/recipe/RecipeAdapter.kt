package com.liang.fridgerecipe.ui.recipe

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.ItemRecipeBinding
import com.liang.fridgerecipe.domain.RecipeMatch

class RecipeAdapter(
    private val onItemClick: (RecipeMatch) -> Unit
) : ListAdapter<RecipeMatch, RecipeAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val match = getItem(position)
        val binding = holder.binding
        val context = binding.root.context

        binding.tvEmoji.text = match.recipe.emoji
        binding.tvName.text = match.recipe.name
        binding.tvMeta.text = "${match.recipe.minutesText} · ${match.recipe.difficultyText}"
        binding.tvScore.text = "${match.score} 分"
        binding.tvReason.text = match.reason
        binding.tvCoverage.text = "主料 ${match.coverageText}"

        val color = if (match.canCook) {
            context.getColor(R.color.green_primary)
        } else {
            context.getColor(R.color.orange_urgent)
        }
        val bg = if (match.canCook) {
            context.getColor(R.color.green_light)
        } else {
            context.getColor(R.color.orange_light)
        }

        binding.tvScore.setTextColor(color)
        binding.tvScore.backgroundTintList = ColorStateList.valueOf(bg)
        binding.tvCoverage.setTextColor(color)
        binding.tvCoverage.backgroundTintList = ColorStateList.valueOf(bg)
        binding.progressMatch.progress = match.score
        binding.progressMatch.progressTintList = ColorStateList.valueOf(color)

        binding.root.setOnClickListener { onItemClick(match) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RecipeMatch>() {
            override fun areItemsTheSame(oldItem: RecipeMatch, newItem: RecipeMatch): Boolean =
                oldItem.recipe.id == newItem.recipe.id

            override fun areContentsTheSame(oldItem: RecipeMatch, newItem: RecipeMatch): Boolean =
                oldItem == newItem
        }
    }
}
