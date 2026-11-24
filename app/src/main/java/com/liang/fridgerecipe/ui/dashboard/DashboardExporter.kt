package com.liang.fridgerecipe.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import com.liang.fridgerecipe.domain.FridgeStats
import com.liang.fridgerecipe.model.Ingredient
import java.io.File

/**
 * P2 伴生模块的「文件导出」能力：
 * 1. 把看板统计导成 CSV（Excel 可直接打开，带 UTF-8 BOM 防乱码）
 * 2. 把自定义柱状图导成 PNG（用 View.draw 抓到 Bitmap，无需截图权限）
 * 3. 通过 FileProvider 分享，文件落在 App 专属外部目录，不需要存储权限
 */
object DashboardExporter {

    private fun exportDir(context: Context): File {
        val dir = context.getExternalFilesDir("exports") ?: File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun exportCsv(context: Context, stats: FridgeStats, priority: List<Ingredient>): File {
        val file = File(exportDir(context), "冰箱菜谱-看板数据-${timestamp()}.csv")
        val builder = StringBuilder()
        builder.append("统计项,数值\n")
        builder.append("食材总数,${stats.total}\n")
        builder.append("已过期,${stats.expired}\n")
        builder.append("今明到期,${stats.urgent}\n")
        builder.append("快到期,${stats.soon}\n")
        builder.append("新鲜,${stats.fresh}\n")
        builder.append("预计浪费率,${stats.wasteRate}%\n")
        builder.append("\n分类,数量\n")
        stats.byCategory.forEach { item -> builder.append("${item.category.label},${item.count}\n") }
        builder.append("\n优先处理食材,分类\n")
        priority.forEach { item -> builder.append("${item.name},${item.category.label}\n") }

        // UTF-8 BOM：Excel 打开中文 CSV 不乱码
        file.writeText("\uFEFF$builder", Charsets.UTF_8)
        return file
    }

    fun exportChart(context: Context, chartView: View): File {
        val width = chartView.width.takeIf { it > 0 } ?: 1080
        val height = chartView.height.takeIf { it > 0 } ?: 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        chartView.draw(canvas)

        val file = File(exportDir(context), "冰箱菜谱-品类分布-${timestamp()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    fun share(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = when (file.extension.lowercase()) {
            "csv" -> "text/csv"
            "png" -> "image/png"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享看板导出文件"))
    }

    private fun timestamp(): String {
        val now = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.getDefault())
        return now.format(java.util.Date())
    }
}
