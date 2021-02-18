package com.lollipop.windowslauncher.tile.impl

import android.content.Context
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.TileType
import com.lollipop.windowslauncher.utils.IconHelper
import com.lollipop.windowslauncher.utils.doAsync
import com.lollipop.windowslauncher.utils.onUI

/**
 * @author lollipop
 * @date 1/31/21 15:43
 * 应用的磁块信息
 */
class AppTile(var appInfo: IconHelper.AppInfo): Tile {

    override var size = TileSize.S

    override val tileType = TileType.App

    fun loadLabel(context: Context, run: (value: CharSequence) -> Unit) {
        val label = appInfo.optLabel()
        if (label != null) {
            run(label)
            return
        }
        doAsync {
            val newLabel = appInfo.getLabel(context)
            onUI {
                run(newLabel)
            }
        }
    }

}