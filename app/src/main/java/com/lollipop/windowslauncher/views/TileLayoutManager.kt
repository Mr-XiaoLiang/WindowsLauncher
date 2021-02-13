package com.lollipop.windowslauncher.views

import android.graphics.Point
import android.graphics.Rect
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.utils.biggerThen
import com.lollipop.windowslauncher.utils.smallerThen

/**
 * @author lollipop
 * @date 1/31/21 21:09
 *
 * 砖块的布局管理器
 * 由于考虑到桌面的场景，因此暂未实现Item的复用
 */
class TileLayoutManager(
    private var spanCount: Int,
    tileSizeProvider: ((Int) -> TileSize),
    insetProvider: InsetsProvider
) : RecyclerView.LayoutManager() {

    private var orientation = Orientation.Vertical

    private val layoutInsets = Rect()

    private var scrollOffset = 0

    private val maxScrollOffset: Int
        get() {
            return (layoutHelper.getContentLength(tileSideLength)
                    - height
                    + startOffsetByOrientation(orientation)
                    + endOffsetByOrientation(orientation))
        }

    private val tileSideLength: Int
        get() {
            return if (orientation.isVertical) {
                width / spanCount
            } else {
                height / spanCount
            }
        }

    private val boundsStart: Int
        get() {
            return scrollOffset
        }

    private val boundsEnd: Int
        get() {
            return boundsStart + height
        }

    private val layoutHelper = TileLayoutHelper(
        ::spanCount,
        ::getItemCount,
        ::orientation,
        tileSizeProvider,
        insetProvider,
    )

    private fun startOffsetByOrientation(o: Orientation): Int {
        if (o != orientation) {
            return 0
        }
        return if (o.isVertical) {
            layoutInsets.top
        } else {
            layoutInsets.left
        }
    }

    private fun endOffsetByOrientation(o: Orientation): Int {
        if (o != orientation) {
            return 0
        }
        return if (o.isVertical) {
            layoutInsets.bottom
        } else {
            layoutInsets.right
        }
    }

    fun setSpanCount(value: Int) {
        this.spanCount = value
        requestLayout()
    }

    fun setOrientation(value: Orientation) {
        this.orientation = value
        requestLayout()
    }

    fun setStartPadding(left: Int, top: Int, right: Int, bottom: Int) {
        layoutInsets.set(left, top, right, bottom)
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
        val tileWidth = tileSideLength
        val verticalOffset = startOffsetByOrientation(Orientation.Vertical)
        val horizontalOffset = startOffsetByOrientation(Orientation.Horizontal)
        for (i in 0 until itemCount) {
            val block = layoutHelper.getBlock(i)
            if (!block.isActive) {
                continue
            }
            val view = recycler.getViewForPosition(i)
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
                block.getLeft(tileWidth) + horizontalOffset,
                block.getTop(tileWidth) + verticalOffset,
                block.getRight(tileWidth) + horizontalOffset,
                block.getBottom(tileWidth) + verticalOffset,
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

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        recycler ?: return 0
        val offset = checkScrollOffset(dy, recycler)
        offsetChildrenVertical(offset * -1)
        return offset
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        recycler ?: return 0
        val offset = checkScrollOffset(dx, recycler)
        offsetChildrenHorizontal(offset * -1)
        return offset
    }

    private fun checkScrollOffset(offset: Int, recycler: RecyclerView.Recycler): Int {
        var newOffset = scrollOffset + offset
        if (newOffset < 0) {
            newOffset = 0
        } else if (newOffset > maxScrollOffset) {
            newOffset = maxScrollOffset
        }
        val actual = newOffset - scrollOffset
        scrollOffset = newOffset
        return actual
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

        private var contentLength = 0

        private fun getLast(index: Int): Int {
            if (index < 0
                || index >= spanCount
                || index >= lastList.size
            ) {
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
            if (contentLength < last) {
                contentLength = last
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
            contentLength = 0
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

        fun getContentLength(tileWidth: Int): Int {
            return contentLength * tileWidth
        }

        data class Block(
            val x: Int,
            val y: Int,
            val size: TileSize,
            val insets: Rect = Rect()
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

            val isActive: Boolean
                get() {
                    return x >= 0 && y >= 0
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