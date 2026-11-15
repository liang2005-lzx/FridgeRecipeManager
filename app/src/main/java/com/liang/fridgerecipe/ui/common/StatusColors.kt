package com.liang.fridgerecipe.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.domain.ExpiryStatus

/** 状态颜色统一在这里映射，列表 / 详情 / 看板共用一套 */
object StatusColors {

    fun textColor(context: Context, status: ExpiryStatus): Int = ContextCompat.getColor(
        context,
        when (status) {
            ExpiryStatus.EXPIRED -> R.color.red_expired
            ExpiryStatus.URGENT -> R.color.orange_urgent
            ExpiryStatus.SOON -> R.color.yellow_soon
            ExpiryStatus.FRESH -> R.color.green_primary
        }
    )

    fun backgroundColor(context: Context, status: ExpiryStatus): Int = ContextCompat.getColor(
        context,
        when (status) {
            ExpiryStatus.EXPIRED -> R.color.red_light
            ExpiryStatus.URGENT -> R.color.orange_light
            ExpiryStatus.SOON -> R.color.yellow_light
            ExpiryStatus.FRESH -> R.color.green_light
        }
    )
}
