package com.lollipop.windowslauncher.tile

import android.content.Context
import android.view.View
import com.lollipop.windowslauncher.tile.tileView.AppTileView
import com.lollipop.windowslauncher.tile.view.TileLayout
import com.lollipop.windowslauncher.tile.view.TileView

/**
 * @author lollipop
 * @date 2/17/21 16:40
 */
class TileViewCreator: TileLayout.TileCreator {

    override fun createTile(tile: Tile, context: Context): TileView<*> {
        when(tile.tileType) {
            TileType.App -> {
                return AppTileView(context)
            }
        }
    }

}