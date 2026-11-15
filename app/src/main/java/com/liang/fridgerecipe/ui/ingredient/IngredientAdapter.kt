package com.liang.fridgerecipe.ui.ingredient

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liang.fridgerecipe.databinding.ItemIngredientBinding
import com.liang.fridgerecipe.domain.ExpiryAnalyzer
import com.liang.fridgerecipe.domain.ExpiryStatus
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.ui.common.StatusColors
import com.liang.fridgerecipe.util.DateUtils

class IngredientAdapter(
    private val onItemClick: (Ingredient) -> Unit
) : ListAdapter<Ingredient, IngredientAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemIngredientBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemIngredientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val binding = holder.binding
        val context = binding.root.context
        val info = ExpiryAnalyzer.info(item)

        binding.tvEmoji.text = item.category.emoji
        binding.tvName.text = item.name
        binding.tvStatus.text = info.status.label

        val textColor = StatusColors.textColor(context, info.status)
        val bgColor = StatusColors.backgroundColor(context, info.status)
        binding.tvStatus.setTextColor(textColor)
        binding.tvStatus.backgroundTintList = ColorStateList.valueOf(bgColor)

        binding.tvAmount.text = context.getString(
            com.liang.fridgerecipe.R.string.ingredient_amount_format,
            item.category.label,
            item.quantityText,
            item.unit
        )
        binding.tvDays.text = if (info.status == ExpiryStatus.EXPIRED) {
            "已经过期了，建议尽快处理"
        } else {
            DateUtils.daysLeftText(info.daysLeft)
        }

        binding.progressRisk.progress = info.risk
        binding.progressRisk.progressTintList = ColorStateList.valueOf(textColor)

        binding.root.setOnClickListener { onItemClick(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Ingredient>() {
            override fun areItemsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean =
                oldItem == newItem
        }
    }
}
