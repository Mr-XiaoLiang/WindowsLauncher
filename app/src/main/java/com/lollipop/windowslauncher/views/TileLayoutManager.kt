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
    private var spanCount: Int,
    tileSizeProvider: ((Int) -> TileSize),
    insetProvider: InsetsProvider
) : RecyclerView.LayoutManager() {

    private val layoutHelper = TileLayoutHelper(
        ::spanCount,
        ::getItemCount,
        ::orientation,
        tileSizeProvider,
        insetProvider
    )

    private var orientation = Orientation.Vertical

    fun setSpanCount(value: Int) {
        this.spanCount = value
        requestLayout()
    }

    fun setOrientation(value: Orientation) {
        this.orientation = value
        requestLayout()
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

        layoutBounds()

        layoutChildren(recycler)
    }

    private fun layoutBounds() {
        layoutHelper.relayout()
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
            val block = layoutHelper.getBlock(i)
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
            addView(view)
            measureChild(view, usedWidth + block.insetHorizontal, usedHeight + block.insetVertical)
            layoutDecorated(
                view,
                block.getLeft(tileWidth),
                block.getTop(tileWidth),
                block.getRight(tileWidth),
                block.getBottom(tileWidth),
            )
        }
    }

    enum class Orientation {
        Vertical,
        Horizontal;

        val isVertical: Boolean
            get() {
                return this == Vertical
            }
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

    override fun canScrollVertically(): Boolean {
        return orientation.isVertical
    }

    override fun canScrollHorizontally(): Boolean {
        return !orientation.isVertical
    }

    class TileLayoutHelper(
        private val spanCountProvider: () -> Int,
        private val itemCountProvider: () -> Int,
        private val orientationProvider: () -> Orientation,
        private val tileSizeProvider: ((Int) -> TileSize),
        private val insetProvider: InsetsProvider,
    ) {

        companion object {
            private val EMPTY = Block(-1, -1, TileSize.S)
        }

        private val spanCount: Int
            get() {
                return spanCountProvider()
            }

        private val itemCount: Int
            get() {
                return itemCountProvider()
            }

        private val orientation: Orientation
            get() {
                return orientationProvider()
            }

        private val lastList = ArrayList<Int>(spanCount)

        private val blockList = ArrayList<Block>()

        private fun getLast(index: Int): Int {
            if (index < 0 || index >= spanCount) {
                return 0
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

        private fun addBlock(point: Point, size: TileSize, insets: Rect) {
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
            blockList.add(Block(point.x, point.y, size, insets))
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
                if (index < 0 ||
                    offset < 0 ||
                    (min in 0 until offset && offset > min)
                ) {
                    index = i
                    offset = min
                }
            }
            return Point(index, offset)
        }

        fun relayout() {
            lastList.clear()
            blockList.clear()
            val insets = Rect()
            for (i in 0 until itemCount) {
                val tileSize = tileSizeProvider(i)
                val space = findSpace(
                    if (orientation.isVertical) {
                        tileSize.width
                    } else {
                        tileSize.height
                    }
                )
                if (space.x < 0 || space.y < 0) {
                    continue
                }
                insets.set(0, 0, 0, 0)
                insetProvider.getInsets(insets, space.x, space.y, tileSize, orientation)
                addBlock(space, tileSize, Rect(insets))
            }
        }

        fun getBlock(index: Int): Block {
            if (index < 0 || index >= blockList.size) {
                return EMPTY
            }
            return blockList[index]
        }

        data class Block(
            var x: Int,
            var y: Int,
            var size: TileSize,
            var insets: Rect = Rect()
        ) {
            fun getLeft(tileWidth: Int): Int {
                return x * tileWidth + insets.left
            }

            fun getTop(tileWidth: Int): Int {
                return y * tileWidth + insets.top
            }

            fun getRight(tileWidth: Int): Int {
                return (x + size.width) * tileWidth - insets.right
            }

            fun getBottom(tileWidth: Int): Int {
                return (y + size.height) * tileWidth - insets.bottom
            }

            val insetHorizontal: Int
                get() {
                    return insets.left + insets.right
                }
            val insetVertical: Int
                get() {
                    return insets.top + insets.bottom
                }
        }

    }

    fun interface InsetsProvider {
        fun getInsets(
            insets: Rect,
            x: Int,
            y: Int,
            size: TileSize,
            orientation: Orientation
        )
    }

}