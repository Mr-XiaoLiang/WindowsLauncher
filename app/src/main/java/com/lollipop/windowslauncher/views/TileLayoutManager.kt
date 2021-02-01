package com.lollipop.windowslauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.TileSize

/**
 * @author lollipop
 * @date 1/31/21 21:09
 */
class TileLayoutManager: RecyclerView.LayoutManager() {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return TileLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
    }



    class TileLayoutParams: RecyclerView.LayoutParams {

        constructor(c: Context?, attrs: AttributeSet?): super(c, attrs)
        constructor(width: Int, height: Int): super(width, height)
        constructor(source: MarginLayoutParams?): super(source) {
            copyParams(source)
        }
        constructor(source: ViewGroup.LayoutParams?): super(source) {
            copyParams(source)
        }
        constructor(source: RecyclerView.LayoutParams?): super(source) {
            copyParams(source)
        }

        private fun copyParams(source: ViewGroup.LayoutParams?) {
            if (source is TileLayoutParams) {
                this.tileSize = source.tileSize
            }
        }

        var tileSize = TileSize.S

    }

}