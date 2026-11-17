package com.liang.fridgerecipe.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.liang.fridgerecipe.BuildConfig
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.model.Recipe
import com.liang.fridgerecipe.model.RecipeIngredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 联网能力②：调用 DeepSeek，根据冰箱里现有的食材生成一道菜谱。
 *
 * Key 通过 local.properties -> BuildConfig 注入，源码里不出现明文；
 * 未配置 Key 时直接返回可读错误，界面提示去配置，不会崩。
 */
class AiRecipeService(
    private val apiKey: String = BuildConfig.DEEPSEEK_API_KEY,
    private val client: OkHttpClient = defaultClient
) {

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    suspend fun generateRecipe(stock: List<Ingredient>): Result<Recipe> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) {
                throw IllegalStateException("未配置 AI Key：请在 local.properties 里填写 DEEPSEEK_API_KEY 后重新构建")
            }
            if (stock.isEmpty()) {
                throw IllegalStateException("冰箱里还没有食材，先添加食材再让 AI 推荐")
            }

            val stockText = stock.take(12).joinToString("、") {
                "${it.name} ${it.quantityText}${it.unit}"
            }

            val requestJson = JsonObject().apply {
                addProperty("model", "deepseek-chat")
                addProperty("temperature", 0.7)
                add("messages", com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty(
                            "content",
                            "你是一名家常菜厨师，只输出 JSON，不要输出 Markdown 代码块或多余解释。"
                        )
                    })
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", buildPrompt(stockText))
                    })
                })
            }

            val body = Gson().toJson(requestJson)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.code == 401) throw IOException("AI Key 无效，请检查 local.properties 里的 DEEPSEEK_API_KEY")
                if (!response.isSuccessful) throw IOException("AI 服务返回 ${response.code}，请稍后再试")

                val content = Gson().fromJson(raw, AiChatResponse::class.java)
                    ?.choices?.firstOrNull()?.message?.content
                    ?: throw IOException("AI 返回内容为空")

                parseRecipe(content)
            }
        }
    }

    private fun buildPrompt(stockText: String): String =
        """
        我冰箱里现在有这些食材：$stockText。
        请只用这些食材（可以额外用盐、油、生抽等基础调料）设计一道简单家常菜。
        严格按下面的 JSON 结构输出，不要加代码块标记：
        {"name":"菜名","emoji":"一个食物emoji","category":"家常菜","minutes":20,"difficulty":1,
         "tips":"一句话小贴士",
         "ingredients":[{"name":"食材名","amount":"用量","optional":false}],
         "steps":["第一步","第二步"]}
        """.trimIndent()

    /** 模型偶尔会带 ```json 包裹，这里做一次容错清洗 */
    private fun parseRecipe(content: String): Recipe {
        val cleaned = content
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val dto = runCatching {
            Gson().fromJson(cleaned, RemoteRecipe::class.java)
        }.getOrNull() ?: throw IOException("AI 返回的不是合法菜谱 JSON，可以再试一次")

        val recipe = dto.toRecipe()
        if (recipe.name.isBlank()) throw IOException("AI 没有给出菜名，可以再试一次")
        if (recipe.steps.isEmpty()) throw IOException("AI 没有给出做法步骤，可以再试一次")

        // 保证至少有一步、用料不为空，避免保存进数据库后详情页空白
        return recipe.copy(
            ingredients = recipe.ingredients.ifEmpty {
                listOf(RecipeIngredient(name = "按现有食材", amount = "适量", optional = true))
            }
        )
    }

    companion object {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
