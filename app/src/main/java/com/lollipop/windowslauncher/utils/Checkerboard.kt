package com.lollipop.windowslauncher.utils

/**
 * @author lollipop
 * @date 2/14/21 00:25
 * 一个棋盘的占位检查工具
 * 它的优势在于快速检索空行（一维数组遍历与二维数组遍历的差距）
 * 插入，删除，检查时，它的速度可能略低于二维数组
 */
class Checkerboard {

    companion object {

        const val LINE_MAX = 32

        private const val LINE_FAST = LINE_MAX - 1

        val rangeArray = IntArray(LINE_MAX + 1) {
            var a = 0
            for (i in 0 until it) {
                a = (a shl 1) or 1
            }
            return@IntArray a
        }
    }

    private val gridMap = ArrayList<Int>()

    fun put(left: Int, top: Int, width: Int = 1, height: Int = 1) {
        if (left + width >= LINE_MAX) {
            throw IndexOutOfBoundsException("The maximum x is 31")
        }
        if (top < 0 || left < 0) {
            return
        }
        checkSize(top + height)
        for (y in 0 until height) {
            gridMap[top + y] = gridMap[top + y].putLineValue(left, width)
        }
    }

    fun remove(left: Int, top: Int, width: Int = 1, height: Int = 1) {
        if (left + width >= LINE_MAX) {
            throw IndexOutOfBoundsException("The maximum x is 31")
        }
        if (gridMap.size <= top || top < 0 || left < 0) {
            return
        }
        checkSize(top + height)
        for (y in 0 until height) {
            gridMap[top + y] = gridMap[top + y].removeLineValue(left, width)
        }
    }

    fun clear() {
        gridMap.clear()
    }

    fun findEmptyLine(): Int {
        for (index in 0 until gridMap.size) {
            if (gridMap[index] == 0) {
                return index
            }
        }
        return -1
    }

    fun removeLine(index: Int) {
        if (index < 0 || index >= gridMap.size) {
            return
        }
        gridMap.removeAt(index)
    }

    fun contains(left: Int, top: Int, width: Int = 1, height: Int = 1): Boolean {
        if (left + width >= LINE_MAX) {
            throw IndexOutOfBoundsException("The maximum x is 31")
        }
        // 棋盘太小，不存在
        if (top < 0 || left < 0 || width < 1
            || height < 1 || gridMap.size <= (height + top)) {
            return false
        }
        for (y in 0 until height) {
            if (gridMap[y + top].hasLineValue(left, width)) {
                return true
            }
        }
        return false
    }

    private fun Int.putLineValue(x: Int, span: Int): Int {
        return this or lineValue(x, span)
    }

    private fun Int.hasLineValue(x: Int, span: Int): Boolean {
        return (this and lineValue(x, span)) != 0
    }

    private fun Int.removeLineValue(x: Int, span: Int): Int {
        return this and (lineValue(x, span).inv())
    }

    private fun lineValue(x: Int, span: Int): Int {
        return rangeArray[span] shl (LINE_FAST - x - span + 1)
    }

    private fun checkSize(y: Int) {
        while (gridMap.size <= y) {
            gridMap.add(0)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder("Checkerboard")
        builder.append("\n")
        builder.append("--------------------------------")
        builder.append("\n")
        for (line in gridMap) {
            val lineValue = line.toUInt().toString(2)
            for (i in 0 until (LINE_MAX - lineValue.length)) {
                builder.append("0")
            }
            builder.append(lineValue)
            builder.append("\n")
        }
        builder.append("--------------------------------")
        return builder.toString()
    }

}