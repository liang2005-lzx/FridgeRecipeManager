package com.liang.fridgerecipe.data.remote

import com.google.gson.Gson
import com.liang.fridgerecipe.BuildConfig
import com.liang.fridgerecipe.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 联网能力①：从云端拉取菜谱库（远程 JSON）。
 *
 * 失败一律抛异常，由 ViewModel 转成"离线模式 + 可重试"的界面状态，
 * 本地已有数据不受影响。
 */
class RecipeSyncService(
    private val url: String = BuildConfig.RECIPES_REMOTE_URL,
    private val client: OkHttpClient = defaultClient
) {

    suspend fun fetchRecipes(): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FridgeRecipe/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("服务器返回 ${response.code}，请稍后再试")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) throw IOException("云端返回内容为空")

                val parsed = runCatching {
                    Gson().fromJson(body, Array<RemoteRecipe>::class.java)
                }.getOrNull() ?: throw IOException("云端数据格式不正确")

                val recipes = parsed.map { it.toRecipe() }
                    .filter { it.name.isNotBlank() && it.ingredients.isNotEmpty() }

                if (recipes.isEmpty()) throw IOException("云端没有可用的菜谱数据")
                recipes
            }
        }
    }

    companion object {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
