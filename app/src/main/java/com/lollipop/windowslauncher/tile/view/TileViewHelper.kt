package com.lollipop.windowslauncher.tile.view

import android.graphics.drawable.Animatable
import com.lollipop.windowslauncher.tile.Tile

/**
 * @author lollipop
 * @date 2/15/21 17:11
 * 磁块的View辅助类
 */
class TileViewHelper(private val tileView: TileView): TileView {

    private val tileAnimatorList = ArrayList<Animatable>()

    private var myTile: Tile? = null

    override fun onResume() {
        tileAnimatorList.forEach {
            it.start()
        }
    }

    override fun onPause() {
        tileAnimatorList.forEach {
            it.stop()
        }
    }

    override fun onBind(tile: Tile) {
        myTile = tile
    }

    fun notifyTileChange() {
        myTile?.let {
            tileView.bind(it)
        }
    }

}