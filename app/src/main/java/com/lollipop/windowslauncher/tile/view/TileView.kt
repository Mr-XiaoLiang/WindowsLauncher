package com.lollipop.windowslauncher.tile.view

import android.view.View
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.tile.Tile

/**
 * @author lollipop
 * @date 2/15/21 16:40
 * 磁块展示的View
 */
interface TileView {

    /**
     * 磁块的View可能包含动画
     * 需要在View被展示时激活
     */
    fun onResume()

    /**
     * 磁块的View可能包含动画
     * 需要在View被隐藏时暂停
     */
    fun onPause()

    /**
     * 绑定tile数据
     */
    fun bind(tile: Tile) {
        if (this is View) {
            this.setBackgroundColor(LColor.tileBackground)
        }
        onBind(tile)
    }

    /**
     * 绑定tile数据
     */
    fun onBind(tile: Tile)

    /**
     * 移动至
     */
    fun moveTo(x: Int, y: Int, delay: Long = 0)

}