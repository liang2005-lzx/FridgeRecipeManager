package com.example.recyclerviewtest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecipeAdapter(private val list: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.VH>() {
    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvMaterial: TextView = itemView.findViewById(R.id.tvMaterial)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.tvName.text = item.name
        holder.tvMaterial.text = item.material
        holder.tvTime.text = item.time
        holder.tvDesc.text = item.desc
    }

    override fun getItemCount(): Int = list.size
}

