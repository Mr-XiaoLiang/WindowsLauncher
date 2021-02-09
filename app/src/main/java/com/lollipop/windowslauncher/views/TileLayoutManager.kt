package com.lollipop.windowslauncher.views

import android.graphics.Point
import android.graphics.Rect
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.TileType

/**
 * @author lollipop
 * @date 1/31/21 21:09
 */
class TileLayoutManager(
    private val tileSizeProvider: ((Int) -> TileSize)
) : RecyclerView.LayoutManager() {

    private var spanCount = 1

    private var orientation = Orientation.Vertical

    private val lastList = ArrayList<Int>(spanCount)

    private val blockList = ArrayList<Block>()

    fun setSpanCount(value: Int) {
        this.spanCount = value
        lastList.clear()
        blockList.clear()
        requestLayout()
    }

    fun setOrientation(value: Orientation) {
        this.orientation = value
        lastList.clear()
        blockList.clear()
        requestLayout()
    }

    private fun getLast(index: Int): Int {
        if (index < 0 || index >= spanCount) {
            return -1
        }
        if (index >= lastList.size) {
            return 0
        }
        return lastList[index]
    }

    private fun setLast(index: Int, v: Int) {
        if (index < 0 || index >= spanCount) {
            return
        }
        while (lastList.size < spanCount) {
            lastList.add(0)
        }
        lastList[index] = v
    }

    private fun addBlock(point: Point, size: TileSize) {
        val min: Int
        val max: Int
        val last: Int
        if (orientation == Orientation.Vertical) {
            min = point.x
            max = min + size.width
            last = point.y + size.height
        } else {
            min = point.y
            max = min + size.height
            last = point.x + size.width
        }
        for (i in min until max) {
            setLast(i, last)
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
                val ly = getLast(i + k)
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

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        recycler?:return
        state?:return
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }
        if (childCount == 0 && state.isPreLayout) {
            return
        }

        detachAndScrapAttachedViews(recycler)

        layoutBounds(recycler)

        layoutChildren(recycler)
    }

    private fun layoutBounds(recycler: RecyclerView.Recycler) {
        // TODO
    }

    private fun layoutChildren(recycler: RecyclerView.Recycler) {
        // TODO
    }

    private class Block(
        var x: Int,
        var y: Int,
        var size: TileSize,
        var decorInsets: Rect
    )

    enum class Orientation {
        Vertical,
        Horizontal
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

}