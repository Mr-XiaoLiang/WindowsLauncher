package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.TileType

/**
 * @author lollipop
 * @date 1/31/21 21:09
 */
class TileLayout(
    context: Context,
    attributeSet: AttributeSet?,
    style: Int
) : ViewGroup(context, attributeSet, style) {

    constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
    constructor(context: Context): this(context, null)

    private var spanCount = 1

    private var orientation = Orientation.Vertical

    private val lastList = ArrayList<Int>(spanCount)

    private val blockList = ArrayList<Block>()

    private val tileAdapter: Adapter? = null

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        lastList.clear()
        blockList.clear()
        if (orientation == Orientation.Vertical) {
            onMeasureVertical(widthMeasureSpec, heightMeasureSpec)
        } else {
            onMeasureHorizontal(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun onMeasureVertical(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val tileWidth = (widthSize - paddingLeft - paddingRight) * 1F / spanCount

        for (i in 0 until childCount) {

        }

    }

    private fun onMeasureHorizontal(widthMeasureSpec: Int, heightMeasureSpec: Int) {

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (orientation == Orientation.Vertical) {
            onLayoutVertical(r - l, b - t)
        } else {
            onLayoutHorizontal(r - l, b - t)
        }
    }

    private fun onLayoutVertical(w: Int, h: Int) {

    }

    private fun onLayoutHorizontal(w: Int, h: Int) {

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

    abstract class Adapter {

        abstract val tileCount: Int

        abstract fun createTile(tileType: TileType): Holder

        abstract fun getTileType(position: Int): TileType

        open fun bindTile(holder: Holder, tile: Tile) {
            holder.bind(tile)
        }
    }

    interface Holder {
        val view: View
        fun bind(tile: Tile)
    }

}