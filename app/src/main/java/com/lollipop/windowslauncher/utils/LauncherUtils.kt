package com.lollipop.windowslauncher.utils

import android.content.Context

/**
 * @author lollipop
 * @date 2/22/21 23:17
 * 启动器相关的工具类
 */

inline fun IconHelper.AppInfo.loadLabel(
    context: Context,
    crossinline run: (value: CharSequence) -> Unit
) {
    val label = optLabel()
    if (label != null) {
        run(label)
        return
    }
    doAsync {
        val newLabel = getLabel(context)
        onUI {
            run(newLabel)
        }
    }
}