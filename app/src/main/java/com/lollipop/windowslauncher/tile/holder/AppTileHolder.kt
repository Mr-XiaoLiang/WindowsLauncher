package com.lollipop.windowslauncher.tile.holder

import com.lollipop.windowslauncher.databinding.ItemTileAppBinding
import com.lollipop.windowslauncher.tile.TileHolder
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.impl.AppTile
import com.lollipop.windowslauncher.utils.visibleOrGone

/**
 * @author lollipop
 * @date 1/31/21 16:25
 * 应用信息的瓷片Holder
 */
class AppTileHolder(
    private val viewBinding: ItemTileAppBinding
    ): TileHolder<AppTile>(viewBinding.root) {

    override fun onBind(tile: AppTile) {
        viewBinding.tileName.visibleOrGone(
            tile.size == TileSize.S) {
            text = tile.appInfo.getLabel(context)
        }
        viewBinding.appIcon.load(tile.appInfo)
    }

}