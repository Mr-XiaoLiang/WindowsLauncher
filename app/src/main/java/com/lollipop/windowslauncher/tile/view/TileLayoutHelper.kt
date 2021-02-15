package com.lollipop.windowslauncher.tile.view

import android.graphics.Point
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.utils.Checkerboard

/**
 * @author lollipop
 * @date 2/14/21 20:10
 */
class TileLayoutHelper(
    private val spanCountProvider: () -> Int,
    private val tileCountProvider: () -> Int,
    private val sizeProvider: (Int) -> TileSize
) {

    companion object {
        private val EMPTY = Block()
    }

    private val spanCount: Int
        get() {
            return spanCountProvider()
        }
    private val tileCount: Int
        get() {
            return tileCountProvider()
        }
    private val lowestLine = LowestLine(spanCountProvider)
    private val checkerboard = Checkerboard()
    private val blockList = ArrayList<Block>()

    fun relayout() {
        lowestLine.clear()
        checkerboard.clear()
        // 检查并记录位置
        forEachBlock { _, block ->
            // 先放弃没有布局的
            if (block.hasLayout) {
                if (checkLayout(block)) {
                    // 重制重叠的块
                    block.resetLayout()
                } else {
                    // 放置可以放置的块
                    putLayout(block)
                    checkLast(block)
                }
            }
        }
        removeEmptyLine()

        layoutFreeBlock()
    }

    fun onTileAdded() {
        layoutFreeBlock()
    }

    fun onTileRemoved() {
        removeEmptyLine()
    }

    fun getBlock(index: Int): Block {
        if (index < 0 || index >= blockList.size) {
            return EMPTY
        }
        return blockList[index]
    }

    fun clear() {
        lowestLine.clear()
        checkerboard.clear()
        blockList.clear()
    }

    private fun removeEmptyLine() {
        // 移除空行
        do {
            val index = checkerboard.findEmptyLine()
            if (index >= 0) {
                checkerboard.removeLine(index)
                lowestLine.removeLine(index)
                for (i in 0 until spanCount) {
                    val block = getBlock(i)
                    if (block.y > index) {
                        block.offsetY(-1)
                    }
                }
            }
        } while (index >= 0)
    }

    private fun layoutFreeBlock() {
        // 重组游离的块
        forEachBlock { _, block ->
            if (!block.hasLayout) {
                val span = block.size.width
                val space = findSpace(span)
                if (space.x >= 0 && space.y >= 0) {
                    block.appoint(space)
                    checkerboard.put(block.x, block.y, span, block.size.height)
                    lowestLine.addLast(
                        block.x, span, block.y + block.size.height)
                }
            }
        }
    }

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
                val last = lowestLine[i + k]
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

    private fun checkLayout(block: Block): Boolean {
        return checkerboard.contains(block.x, block.y, block.size.width, block.size.height)
    }

    private fun putLayout(block: Block) {
        checkerboard.put(block.x, block.y, block.size.width, block.size.height)
    }

    private fun checkLast(block: Block) {
        val last = block.y + block.size.height
        for (i in 0 until block.size.width) {
            lowestLine.addLast(block.x + i, 1, last)
        }
    }

    private fun forEachBlock(run: (index: Int, block: Block) -> Unit) {
        while (blockList.size < tileCount) {
            blockList.add(Block())
        }
        for (i in 0 until tileCount) {
            blockList[i].size = sizeProvider(i)
            run(i, blockList[i])
        }
    }

    class LowestLine(private val spanCountProvider: () -> Int) {

        private val spanCount: Int
            get() {
                return spanCountProvider()
            }

        private val lineList = ArrayList<Int>(spanCount)

        operator fun get(index: Int): Int {
            if (index < 0
                || index >= spanCount
                || index >= lineList.size) {
                return 0
            }
            return lineList[index]
        }

        operator fun set(index: Int, value: Int) {
            if (index >= spanCount) {
                return
            }
            while (lineList.size < spanCount) {
                lineList.add(0)
            }
            lineList[index] = value
        }

        fun addLast(index: Int, span: Int, value: Int) {
            for (i in 0 until span) {
                val old = get(index + i)
                if (old < value) {
                    set(index + i, value)
                }
            }
        }

        fun setRange(index: Int, span: Int, value: Int) {
            for (i in 0 until span) {
                set(index + i, value)
            }
        }

        fun clear() {
            lineList.clear()
        }

        fun removeLine(index: Int) {
            for (i in 0 until lineList.size) {
                if (lineList[i] > index) {
                    lineList[i] = lineList[i] - 1
                }
            }
        }

    }

    class Block {

        var x: Int = -1
            private set
        var y: Int = -1
            private set
        var size: TileSize = TileSize.S

        val hasLayout: Boolean
            get() {
                return x < 0 || y < 0
            }

        fun resetLayout() {
            this.x = -1
            this.y = -1
        }

        fun offsetY(offset: Int) {
            this.y += offset
        }

        fun appoint(point: Point) {
            this.x = point.x
            this.y = point.y
        }

    }

}