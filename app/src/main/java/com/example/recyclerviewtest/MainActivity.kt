package com.example.recyclerviewtest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.rv_recipe)
        rv.layoutManager = LinearLayoutManager(this)

        val dataList = listOf(
            Recipe("番茄炒蛋", "番茄、鸡蛋", "10分钟", "简单家常菜"),
            Recipe("酸辣土豆丝", "土豆、辣椒", "8分钟", "酸辣爽口")
        )
        val adapter = RecipeAdapter(dataList)
        rv.adapter = adapter
    }
}

