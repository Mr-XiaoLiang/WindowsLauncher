package com.lollipop.windowslauncher.tile.view

import android.graphics.Point
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.utils.Checkerboard
import org.json.JSONArray
import org.json.JSONObject
import java.lang.RuntimeException

/**
 * @author lollipop
 * @date 2/14/21 20:10
 * 磁块排版的辅助工具类
 * 它包含
 * - 必要的排版功能
 * - 动态插入功能
 * - 动态添加功能
 * - 动态移除功能
 * - 位置快照
 * - 快照恢复
 * - 快照对比
 * - 位置撑开
 * - 空行移除
 * - 重叠纠正
 */
class TileLayoutHelper(
    private val spanCountProvider: () -> Int,
    private val tileCountProvider: () -> Int,
    private val sizeProvider: (Int) -> TileSize
) {

    companion object {
        private val EMPTY = Block()
        private val TEMP_BLOCK = Block()

        fun tileWidth(width: Int, spanCount: Int, space: Int): Int {
            return (width - space) / spanCount - space
        }

        fun blockLeft(tileWidth: Int, space: Int, x: Int): Int {
            return blockEdge(tileWidth, space, x)
        }

        fun blockTop(tileWidth: Int, space: Int, y: Int): Int {
            return blockEdge(tileWidth, space, y)
        }

        fun blockWidth(tileWidth: Int, space: Int, size: TileSize): Int {
            return blockSideLength(tileWidth, space, size.width)
        }

        fun blockHeight(tileWidth: Int, space: Int, size: TileSize): Int {
            return blockSideLength(tileWidth, space, size.height)
        }

        private fun blockEdge(tileWidth: Int, space: Int, index: Int): Int {
            return (tileWidth + space) * index + space
        }

        private fun blockSideLength(tileWidth: Int, space: Int, span: Int): Int {
            return (tileWidth + space) * span - space
        }
    }

    /**
     * 列数提供器
     * 允许外部动态调整列数
     * 但是随意的调整大小，可能会造成排版的错乱问题
     */
    private val spanCount: Int
        get() {
            val count = spanCountProvider()
            if (count > Checkerboard.LINE_MAX) {
                throw RuntimeException("Checkerboard max span count is ${Checkerboard.LINE_MAX}")
            }
            if (count < 1) {
                throw RuntimeException("spanCount needs to be greater than 0")
            }
            return count
        }

    /**
     * 磁块的数量，他允许变化
     * 但是变化的时候，最好通知一下布局管理器
     */
    private val tileCount: Int
        get() {
            return tileCountProvider()
        }

    /**
     * 最低线条的管理组件
     */
    private val lowestLine = LowestLine(spanCountProvider)

    /**
     * 棋盘格一样的占位检查组件
     * 他也是空行检查的主要依据
     */
    private val checkerboard = Checkerboard()

    /**
     * 真正的布局管理器
     * 它包含位置信息以及尺寸信息
     * 它的位置会在内部被确定，并且作为依据提供给外部
     */
    private val blockList = ArrayList<Block>()

    /**
     * 最大行数
     */
    val maxLine: Int
        get() {
            return lowestLine.maxLine
        }

    /**
     * 是否自动同步块的尺寸
     */
    private var syncSize = true

    fun lock() {
        syncSize = false
    }

    fun unlock() {
        syncSize = true
    }

    fun syncSize() {
        forEachBlock { _, _ ->  }
    }

    fun resetBlock(index: Int, size: TileSize? = null) {
        blockList[index].resetLayout()
        size?.let {
            blockList[index].size = it
        }
    }

    fun relayout() {
        lowestLine.clear()
        checkerboard.clear()
        // 检查并记录位置
        forEachBlock { _, block ->
            // 先放弃没有布局的
            if (block.hasLayout && block.active) {
                if (checkLayout(block)) {
                    // 重制重叠的块
                    block.resetLayout()
                } else {
                    // 放置可以放置的块
                    checkerboard.put(block)
                    lowestLine.checkLast(block)
                }
            }
        }
        removeEmptyLine()

        layoutFreeBlock()
    }

    /**
     * 添加磁块时，默认为添加到最后
     * 并且添加磁块默认为自动布局
     * 新添加的磁块一定是没有排版的
     * 并且添加的磁块数量不做要求
     */
    fun notifyTileAdded() {
        layoutFreeBlock()
    }

    /**
     * 移除磁块时，不论移除多少块
     * 默认为期间没有尺寸变化和新的添加
     * 这时候只检查空行及更新最后标准线
     */
    fun notifyTileRemoved(indexArray: IntArray) {
        for (index in indexArray) {
            val block = blockList[index]
            block.callRemove()
            checkerboard.remove(block.x, block.y, block.size.width, block.size.height)
        }
        lowestLine.checkLast()
        removeEmptyLine()
    }

    /**
     * 插入磁块
     * 此时默认为指定序号的磁块为基础进行插入
     * 此时可以设置序号，位置，尺寸，并且进行磁块位置的偏移计算
     */
    fun notifyTileInsert(index: Int, x: Int, y: Int, size: TileSize): Boolean {
        val pushStatus = pushTile(x, y, size)
        if (!pushStatus) {
            return false
        }
        blockList.add(index, Block().apply {
            appoint(x, y)
            this.size = size
        })
        checkerboard.put(x, y, size.width, size.height)
        notifyTileAdded()
        return true
    }

    /**
     * 推开磁块
     * 它将会在指定的位置推开一个可以容纳指定TileSize的空间
     * 但是它不会放入内容物
     * 它主要用于几个场景：插入时，拖拽磁块时
     * @return 如果指定的位置不足以放下指定的块，那么将会返回false
     */
    fun pushTile(x: Int, y: Int, size: TileSize, keepBlock: Int = -1): Boolean {
        // 如果已经可用，那么放弃操作
        if (!checkerboard.contains(x, y, size.width, size.height)) {
            return true
        }
        // 否则的话，进行块遍历，并且全部迁移
        var left = x
        var right = left + size.width - 1
        val offsetY = size.height
        if (right > spanCount) {
            return false
        }
        val movedBlock = ArrayList<Block>()
        // 需要一行一行的处理，因为这是金字塔形状的干涉
        for (line in y..lowestLine.maxLine) {
            // 遍历寻找同一行的元素，进行偏移
            forEachBlock { index, block ->
                if (keepBlock != index
                    && block.y == line
                    && block.overlap(left, right)
                    && !movedBlock.contains(block)
                ) {
                    if (block.x < left) {
                        left = block.x
                    }
                    if (block.x + block.size.width > right) {
                        right = block.x + block.size.width
                    }
                    // 移动时不能忘了棋盘格，并且这时候不方便做移动，因此采取先移除再加入的方式
                    checkerboard.remove(block)
                    block.offsetY(offsetY)
                    movedBlock.add(block)
                    checkerboard.put(block)
                }
            }
        }
        // 最后检查一次最低线条的位置
        lowestLine.checkLast()
        return true
    }

    /**
     * 获取当前状态的快照信息
     * 快照信息会被锁定，处于只读状态
     */
    fun getSnapshot(): Snapshot {
        val snapshot = Snapshot()
        forEachBlock { _, block ->
            snapshot.add(block.x, block.y, !block.pendingRemove, block.size)
        }
        snapshot.lock()
        return snapshot
    }

    /**
     * 获取当前位置状态与指定快照之间的差异
     * 并且通过回调函数传递出去
     * 目的在于方便UI进行更新同步（只变更变化的部分）
     */
    fun diff(
        snapshot: Snapshot,
        run: (index: Int, block: Block) -> Unit
    ) {
        forEachBlock { index, block ->
            if (index < snapshot.size) {
                val readX = snapshot.readX(index)
                val readY = snapshot.readY(index)
                val readSize = snapshot.readSize(index)
                if (readX != block.x || readY != block.y || readSize != block.size) {
                    run(index, block)
                }
            }
        }
    }

    /**
     * 获取一个指定序号的块信息
     * 它主要是获取位置信息
     * 它将返回一个临时的block，用于传递位置信息
     * 这是为了避免外部的参数设置导致的不必要的内部逻辑混乱
     */
    fun getBlock(index: Int): Block {
        val block = TEMP_BLOCK
        if (index < 0 || index >= blockList.size) {
            EMPTY.paste(block)
            return block
        }
        blockList[index].paste(block)
        return block
    }

    /**
     * 清理
     * 他会清理所有排版的记录数据
     * 比如最低线，棋盘占位，block位置等
     */
    fun clear() {
        lowestLine.clear()
        checkerboard.clear()
        blockList.clear()
    }

    /**
     * 重置布局内容
     * 它会完全重置布局结果，
     * 然后按照重新指定的位置进行一次排版更新
     */
    fun restore(run: (index: Int, block: Block) -> Unit) {
        clear()
        for (i in 0 until tileCount) {
            val block = Block()
            run(i, block)
        }
        relayout()
    }

    /**
     * 自动移除无用的块
     */
    fun autoRemoveBlock(run: (index: Int, block: Block) -> Unit) {
        val removedList = ArrayList<Block>()
        forEachBlock { index, block ->
            if (block.pendingRemove) {
                run(index, block)
                removedList.add(block)
            }
        }
        blockList.removeAll(removedList)
    }

    /**
     * 恢复镜像的布局内容
     * 用于临时排版结束时恢复
     * 或者拖拽磁块时位置移动了，
     * 需要恢复上一次的位置然后重新寻找位置
     */
    fun restore(snapshot: Snapshot) {
        restore { index, block ->
            if (index < snapshot.size) {
                block.appoint(
                    snapshot.readX(index),
                    snapshot.readY(index)
                )
            }
        }
    }

    /**
     * 内容物的高度计算
     */
    fun contentHeight(tileWidth: Int, space: Int): Int {
        return (tileWidth + space) * lowestLine.maxLine + space
    }

    fun syncBlockSize(): Boolean {
        return layoutFreeBlock()
    }

    fun removeEmptyLine(): Boolean {
        // 移除空行
        var hasChanged = false
        do {
            val index = checkerboard.findEmptyLine()
            if (index >= 0) {
                hasChanged = true
                checkerboard.removeLine(index)
                lowestLine.removeLine(index)
                for (i in 0 until tileCount) {
                    val block = getBlock(i)
                    if (block.y > index) {
                        block.offsetY(-1)
                    }
                }
            }
        } while (index >= 0)
        return hasChanged
    }

    private fun layoutFreeBlock(): Boolean {
        var hasChanged = false
        // 重组游离的块
        forEachBlock { _, block ->
            if (!block.hasLayout && block.active) {
                val space = findSpace(block.size.width)
                if (space.x >= 0 && space.y >= 0) {
                    hasChanged = true
                    block.appoint(space)
                    checkerboard.put(block)
                    lowestLine.checkLast(block)
                }
            }
        }
        return hasChanged
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

    private fun Checkerboard.put(block: Block) {
        if (block.pendingRemove) {
            return
        }
        put(block.x, block.y, block.size.width, block.size.height)
    }

    private fun Checkerboard.remove(block: Block) {
        remove(block.x, block.y, block.size.width, block.size.height)
    }

    private fun LowestLine.checkLast() {
        clear()
        forEachBlock { _, block ->
            checkLast(block)
        }
    }

    private fun LowestLine.checkLast(block: Block) {
        if (block.pendingRemove) {
            return
        }
        addLast(block.x, block.size.width, block.y + block.size.height)
    }

    private fun forEachBlock(run: (index: Int, block: Block) -> Unit) {
        while (blockList.size < tileCount) {
            blockList.add(Block())
        }
        for (i in 0 until tileCount) {
            if (syncSize) {
                blockList[i].size = sizeProvider(i)
            }
            run(i, blockList[i])
        }
    }

    class LowestLine(private val spanCountProvider: () -> Int) {

        private val spanCount: Int
            get() {
                return spanCountProvider()
            }

        private val lineList = ArrayList<Int>(spanCount)

        var maxLine = 0
            private set

        operator fun get(index: Int): Int {
            if (index < 0
                || index >= spanCount
                || index >= lineList.size
            ) {
                return 0
            }
            return lineList[index]
        }

        operator fun set(index: Int, value: Int) {
            if (index < 0 || index >= spanCount) {
                return
            }
            while (lineList.size < spanCount) {
                lineList.add(0)
            }
            lineList[index] = value
            if (maxLine < value) {
                maxLine = value
            }
        }

        fun addLast(index: Int, span: Int, value: Int) {
            for (i in 0 until span) {
                val old = get(index + i)
                if (old < value) {
                    set(index + i, value)
                }
            }
        }

        fun clear() {
            lineList.clear()
            maxLine = 0
        }

        fun removeLine(index: Int) {
            maxLine = 0
            for (i in 0 until lineList.size) {
                var line = lineList[i]
                if (line > index) {
                    lineList[i] = line - 1
                }
                line = lineList[i]
                if (maxLine < line) {
                    maxLine = line
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
                return x >= 0 || y >= 0
            }

        var pendingRemove = false
            private set

        val active: Boolean
            get() {
                return !pendingRemove
            }

        fun resetLayout() {
            if (pendingRemove) {
                return
            }
            this.x = -1
            this.y = -1
        }

        fun offsetY(offset: Int) {
            if (pendingRemove) {
                return
            }
            this.y += offset
        }

        fun appoint(point: Point) {
            appoint(point.x, point.y)
        }

        fun appoint(x: Int, y: Int) {
            if (pendingRemove) {
                return
            }
            this.x = x
            this.y = y
        }

        fun overlap(left: Int, right: Int): Boolean {
            if (pendingRemove) {
                return false
            }
            val x1 = x
            val x2 = x1 + size.width
            if (left <= x1 && right >= x2) {
                return true
            }
            if (left >= x1 && right <= x2) {
                return true
            }
            if (x1 in left..right) {
                return true
            }
            if (x2 in left..right) {
                return true
            }
            return false
        }

        fun paste(block: Block) {
            block.x = this.x
            block.y = this.y
            block.size = this.size
        }

        fun left(tileWidth: Int, space: Int): Int {
            return blockLeft(tileWidth, space, x)
        }

        fun top(tileWidth: Int, space: Int): Int {
            return blockTop(tileWidth, space, y)
        }

        fun right(tileWidth: Int, space: Int): Int {
            return left(tileWidth, space) + width(tileWidth, space)
        }

        fun bottom(tileWidth: Int, space: Int): Int {
            return top(tileWidth, space) + height(tileWidth, space)
        }

        fun width(tileWidth: Int, space: Int): Int {
            return blockWidth(tileWidth, space, size)
        }

        fun height(tileWidth: Int, space: Int): Int {
            return blockHeight(tileWidth, space, size)
        }

        fun callRemove() {
            this.pendingRemove = true
        }

    }

    class Snapshot {

        private var isLock = false

        private val locationList = ArrayList<IntArray>()

        fun add(x: Int, y: Int, active: Boolean, size: TileSize) {
            if (isLock) {
                return
            }
            val activeFlag = if (active) {
                1
            } else {
                0
            }
            locationList.add(intArrayOf(x, y, activeFlag, size.ordinal))
        }

        fun readX(line: Int): Int {
            return locationList[line][0]
        }

        fun readY(line: Int): Int {
            return locationList[line][1]
        }

        fun isActive(line: Int): Boolean {
            return locationList[line][2] > 0
        }

        fun readSize(line: Int): TileSize {
            return TileSize.values()[locationList[line][3]]
        }

        fun lock() {
            this.isLock = true
        }

        val size: Int
            get() {
                return locationList.size
            }

        override fun toString(): String {
            val jsonArray = JSONArray()
            locationList.forEach {
                jsonArray.put(JSONObject().apply {
                    put("x", it[0])
                    put("y", it[1])
                    put("active", it[2])
                    put("size", it[3])
                })
            }
            return jsonArray.toString()
        }

    }

}