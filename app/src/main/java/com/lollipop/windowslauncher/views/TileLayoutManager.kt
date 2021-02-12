package com.lollipop.windowslauncher.views

import android.graphics.Point
import android.graphics.Rect
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.TileSize

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
        if (orientation.isVertical) {
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
        var offset = -1
        for (i in 0 until spanCount) {
            if (i + span > spanCount) {
                break
            }
            var min = -1
            // 取从此处开始最大的值
            for (k in 0 until span) {
                val last = getLast(i + k)
                if (min < last) {
                    min = last
                }
            }
            if (min in 0 until offset) {
                index = i
                offset = min
            }
        }
        return Point(index, offset)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        recycler ?: return
        state ?: return
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
        lastList.clear()
        blockList.clear()
        when (orientation) {
            Orientation.Vertical -> {
                layoutBoundsByVertical(recycler)
            }
            Orientation.Horizontal -> {
                layoutBoundsByHorizontal(recycler)
            }
        }
    }

    private fun layoutBoundsByVertical(recycler: RecyclerView.Recycler) {
        for (i in 0 until itemCount) {
            val tileSize = tileSizeProvider(i)
            val space = findSpace(tileSize.width)
            if (space.x < 0 || space.y < 0) {
                continue
            }
            addBlock(space, tileSize)
        }
    }

    private fun layoutBoundsByHorizontal(recycler: RecyclerView.Recycler) {
        for (i in 0 until itemCount) {
            val tileSize = tileSizeProvider(i)
            val space = findSpace(tileSize.height)
            if (space.x < 0 || space.y < 0) {
                continue
            }
            addBlock(space, tileSize)
        }
    }

    private fun layoutChildren(recycler: RecyclerView.Recycler) {
        val allWidth = width
        val allHeight = height
        val tileWidth = if (orientation.isVertical) {
            allWidth / spanCount
        } else {
            allHeight / spanCount
        }
        for (i in 0 until itemCount) {
            val view = recycler.getViewForPosition(i)
            val block = blockList[i]
            val usedWidth = if (orientation.isVertical) {
                (spanCount - block.size.width) * tileWidth
            } else {
                block.x
            }
            val usedHeight = if (orientation.isVertical) {
                block.y
            } else {
                (spanCount - block.size.height) * tileWidth
            }
            measureChild(view, usedWidth, usedHeight)
            layoutDecorated(view,
                block.getLeft(tileWidth),
                block.getTop(tileWidth),
                block.getRight(tileWidth),
                block.getBottom(tileWidth),
            )
        }
    }

    private class Block(
        var x: Int,
        var y: Int,
        var size: TileSize,
        var decorInsets: Rect = Rect()
    ) {
        fun getLeft(tileWidth: Int): Int {
            return x * tileWidth
        }

        fun getTop(tileWidth: Int): Int {
            return y * tileWidth
        }

        fun getRight(tileWidth: Int): Int {
            return (x + size.width) * tileWidth
        }

        fun getBottom(tileWidth: Int): Int {
            return (y + size.height) * tileWidth
        }
    }

    enum class Orientation {
        Vertical,
        Horizontal
    }

    private val Orientation.isVertical: Boolean
        get() {
            return this == Orientation.Vertical
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