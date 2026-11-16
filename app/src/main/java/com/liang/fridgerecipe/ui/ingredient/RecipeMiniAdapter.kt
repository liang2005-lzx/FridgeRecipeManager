package com.liang.fridgerecipe.ui.ingredient

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.ItemRecipeMiniBinding
import com.liang.fridgerecipe.domain.RecipeMatch

/** 食材详情页里的"可以做的菜"小卡片 */
class RecipeMiniAdapter(
    private val onClick: (RecipeMatch) -> Unit
) : ListAdapter<RecipeMatch, RecipeMiniAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRecipeMiniBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecipeMiniBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val match = getItem(position)
        val binding = holder.binding
        val context = binding.root.context

        binding.tvEmoji.text = match.recipe.emoji
        binding.tvName.text = match.recipe.name
        binding.tvReason.text = match.reason
        binding.tvScore.text = "${match.score} 分"

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

        binding.root.setOnClickListener { onClick(match) }
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
