package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.TileSize

/**
 * @author lollipop
 * @date 1/31/21 21:09
 */
class TileLayoutManager(
    private var spanCount: Int,
    var orientation: Int = RecyclerView.VERTICAL,
    private val infoProvider: TileInfoProvider,
) : RecyclerView.LayoutManager() {

    private val lastYList = ArrayList<Int>(spanCount)

    private val blockList = ArrayList<Block>()

    fun setSpanCount(value: Int) {
        this.spanCount = value
        lastYList.clear()
        blockList.clear()
        requestLayout()
    }

    private fun getLastY(index: Int): Int {
        if (index < 0 || index >= spanCount) {
            return -1
        }
        if (index >= lastYList.size) {
            return 0
        }
        return lastYList[index]
    }

    private fun setLastY(index: Int, y: Int) {
        if (index < 0 || index >= spanCount) {
            return
        }
        while (lastYList.size < spanCount) {
            lastYList.add(0)
        }
        lastYList[index] = y
    }

    private fun addLastY(index: Int, height: Int, span: Int) {
        for (i in 0 until span) {
            setLastY(index, getLastY(index + i) + height)
        }
    }

    /**
     * 寻找一个可用的空间
     */
    private fun findSpace(span: Int): Point {
        var index = -1
        var y = -1
        for (i in 0 until spanCount) {
            if (i + span > spanCount) {
                break
            }
            var min = -1
            // 取从此处开始最大的值
            for (k in 0 until span) {
                val ly = getLastY(i + k)
                if (min < ly) {
                    min = ly
                }
            }
            if (min in 0 until y) {
                index = i
                y = min
            }
        }
        return Point(index, y)
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return TileLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun canScrollHorizontally(): Boolean {
        return orientation == RecyclerView.HORIZONTAL
    }

    override fun canScrollVertically(): Boolean {
        return orientation == RecyclerView.VERTICAL
    }

    override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
        return lp != null && lp is TileLayoutParams
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        state ?: return
        recycler ?: return
        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }
        detachAndScrapAttachedViews(recycler)

        val left = paddingLeft
        val top = paddingTop
        val right = paddingRight
        val bottom = paddingBottom
        val tileWidth = (width - left - right) * 1F / spanCount

        for (i in 0 until itemCount) {
            val view = recycler.getViewForPosition(i)
            val tileSize = infoProvider.getTileSize(i)
            val findSpace = findSpace(tileSize.width)
            val decorInsets = infoProvider.getDecorInsets(i, findSpace.x, findSpace.y, tileSize)

            val itemWidth =
                (tileSize.width * tileWidth - decorInsets.left - decorInsets.right).toInt()
            val itemHeight =
                (tileSize.height * tileWidth - decorInsets.top - decorInsets.bottom).toInt()
            view.measure(
                View.MeasureSpec.makeMeasureSpec(itemWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(itemHeight, View.MeasureSpec.EXACTLY)
            )

            val itemLeft = (tileWidth * findSpace.x + decorInsets.left + left).toInt()
            val itemTop = (tileWidth * findSpace.y + decorInsets.top + top).toInt()
            view.layout(itemLeft, itemTop, itemLeft + itemWidth, itemTop + itemHeight)
        }

    }

    private class Block(
        var x: Int,
        var y: Int,
        var size: TileSize,
        var decorInsets: Rect
    )

    class TileLayoutParams : RecyclerView.LayoutParams {

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: MarginLayoutParams?) : super(source) {
            copyParams(source)
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source) {
            copyParams(source)
        }

        constructor(source: RecyclerView.LayoutParams?) : super(source) {
            copyParams(source)
        }

        private fun copyParams(source: ViewGroup.LayoutParams?) {
            if (source is TileLayoutParams) {
                this.tileSize = source.tileSize
            }
        }

        var tileSize = TileSize.S

    }

    interface TileInfoProvider {

        fun getTileSize(position: Int): TileSize

        fun getDecorInsets(position: Int, x: Int, y: Int, size: TileSize): Rect

    }

}