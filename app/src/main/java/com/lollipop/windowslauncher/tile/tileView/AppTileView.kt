package com.lollipop.windowslauncher.tile.tileView

import android.content.Context
import com.lollipop.windowslauncher.R
import com.lollipop.windowslauncher.databinding.ItemTileAppBinding
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.impl.AppTile
import com.lollipop.windowslauncher.tile.view.TileView
import com.lollipop.windowslauncher.utils.visibleOrGone
import com.lollipop.windowslauncher.utils.withThis

/**
 * @author lollipop
 * @date 2/17/21 16:43
 * App的磁块
 */
class AppTileView(context: Context) : TileView<AppTile>(context) {

    private val viewBinding: ItemTileAppBinding by withThis(R.layout.item_tile_app)

    override fun onBind(tile: AppTile) {
        viewBinding.appIcon.load(tile.appInfo)
        viewBinding.tileName.visibleOrGone(tile.size != TileSize.S) {
            tile.loadLabel(context) {
                text = it
            }
            setTextColor(LColor.foreground)
        }
    }
}