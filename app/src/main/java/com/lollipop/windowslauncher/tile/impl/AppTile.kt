package com.lollipop.windowslauncher.tile.impl

import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.TileType
import com.lollipop.windowslauncher.utils.IconHelper

/**
 * @author lollipop
 * @date 1/31/21 15:43
 * 应用的磁块信息
 */
class AppTile(var appInfo: IconHelper.AppInfo): Tile {

    override var size = TileSize.S

    override val tileType = TileType.App

}