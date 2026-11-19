package com.liang.fridgerecipe.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.domain.CategoryCount
import kotlin.math.max

/**
 * 自定义 View（Canvas 绘制）：冰箱里各分类的库存分布。
 * 没有引入第三方图表库，按课程要求自己算坐标、自己画。
 */
class CategoryBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<CategoryCount> = emptyList()

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = sp(12f)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = sp(12f)
        textAlign = Paint.Align.RIGHT
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_primary)
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider)
    }

    private val rowHeight = dp(30f)
    private val barHeight = dp(9f)
    private val labelWidth = dp(86f)
    private val valueWidth = dp(32f)
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = sp(13f)
        textAlign = Paint.Align.CENTER
    }

    private val barRect = RectF()

    fun setData(list: List<CategoryCount>) {
        data = list
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = max(data.size, 1)
        val desiredHeight = (rows * rowHeight + dp(8f)).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            canvas.drawText(
                "还没有食材，先去冰箱记一笔吧",
                width / 2f,
                height / 2f + dp(4f),
                emptyPaint
            )
            return
        }

        val maxCount = data.maxOf { it.count }.coerceAtLeast(1)
        val trackStart = labelWidth
        val trackEnd = width - valueWidth - dp(8f)
        val trackWidth = (trackEnd - trackStart).coerceAtLeast(dp(20f))

        data.forEachIndexed { index, item ->
            val centerY = rowHeight * index + rowHeight / 2f + dp(4f)
            val top = centerY - barHeight / 2f

            canvas.drawText(
                "${item.category.emoji} ${item.category.label}",
                dp(2f),
                centerY + dp(4f),
                labelPaint
            )

            barRect.set(trackStart, top, trackEnd, top + barHeight)
            canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, trackPaint)

            val ratio = item.count.toFloat() / maxCount.toFloat()
            barRect.set(trackStart, top, trackStart + trackWidth * ratio, top + barHeight)
            canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, barPaint)

            canvas.drawText(
                "${item.count}",
                width.toFloat() - dp(2f),
                centerY + dp(4f),
                valuePaint
            )
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    @Suppress("unused")
    private fun colorWithAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
