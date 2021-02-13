package com.lollipop.windowslauncher.tile

import android.graphics.Point
import android.graphics.Rect
import com.lollipop.windowslauncher.views.TileLayoutManager

/**
 * @author lollipop
 * @date 2/13/21 17:04
 */
class TileLayoutHelper(
    private val spanCountProvider: () -> Int,
    private val itemCountProvider: () -> Int,
    private val orientationProvider: () -> Orientation,
    private val tileSizeProvider: ((Int) -> TileSize),
    private val insetProvider: TileLayoutManager.InsetsProvider,
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

    var contentLength = 0
        private set

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
            if (!orientation.isVertical) {
                val temp = space.x
                space.x = space.y
                space.y = temp
            }
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