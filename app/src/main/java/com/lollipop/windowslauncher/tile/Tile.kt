package com.lollipop.windowslauncher.tile

import android.content.Context

/**
 * @author lollipop
 * @date 1/30/21 14:04
 * 瓷砖的接口
 */
interface Tile {

    var size: TileSize

    val tileType: TileType

    fun loadSync(context: Context)

}