package com.lollipop.windowslauncher.tile.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.tile.Tile

/**
 * @author lollipop
 * @date 2/15/21 16:20
 */
class TileLayout(
    context: Context,
    attributeSet: AttributeSet?,
    style: Int
): ViewGroup(context, attributeSet, style) {

    constructor(context: Context,
                attributeSet: AttributeSet?): this(context, attributeSet, 0)

    constructor(context: Context): this(context, null)

    private val tileList = ArrayList<Tile>()

    private var tileCreator: TileCreator? = null

    private var isActive = false

    var spanCount: Int = 1
        set(value) {
            field = value
            requestLayout()
        }

    private val tileLayoutHelper = TileLayoutHelper(::spanCount, ::getBlock)

    private fun getBlock(index: Int): TileLayoutHelper.Block {
        TODO()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        TODO("Not yet implemented")
    }

    fun removeTileAt(index: Int) {
        tileList.removeAt(index)
        removeViewAt(index)
        tileLayoutHelper.onTileRemoved()
    }

    fun addTile(tiles: List<Tile>) {
        for (tile in tiles) {
            addTile(tile)
        }
    }

    fun addTile(tile: Tile) {
        val view = getTileView(tile)?:return
        tileList.add(tile)
        addView(view)
        if (view is TileView) {
            view.bind(tile)
            checkViewStatus(view)
        }
        tileLayoutHelper.onTileAdded()
    }

    /**
     * 磁块的View可能包含动画
     * 需要在View被展示时激活
     */
    fun onResume() {
        isActive = true
        for (i in 0 until childCount) {
            checkViewStatus(getChildAt(i))
        }
    }

    /**
     * 磁块的View可能包含动画
     * 需要在View被隐藏时暂停
     */
    fun onPause() {
        isActive = false
        for (i in 0 until childCount) {
            checkViewStatus(getChildAt(i))
        }
    }

    private fun checkViewStatus(view: View) {
        if (view is TileView) {
            if (isActive && view.visibility == View.VISIBLE) {
                view.onResume()
            } else {
                view.onPause()
            }
        }
    }

    private fun getTileView(tile: Tile): View? {
        return tileCreator?.createTile(tile)
    }

    fun interface TileCreator {
        fun createTile(tile: Tile): View
    }

}