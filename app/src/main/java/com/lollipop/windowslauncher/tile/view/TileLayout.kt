package com.lollipop.windowslauncher.tile.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.views.ScrollHelper
import kotlin.math.min

/**
 * @author lollipop
 * @date 2/15/21 16:20
 */
class TileLayout(
    context: Context,
    attributeSet: AttributeSet?,
    style: Int
) : ViewGroup(context, attributeSet, style) {

    constructor(
        context: Context,
        attributeSet: AttributeSet?
    ) : this(context, attributeSet, 0)

    constructor(context: Context) : this(context, null)

    private val tileList = ArrayList<Tile>()

    private var tileCreator: TileCreator? = null

    private var isActive = false

    var spanCount: Int = 1
        set(value) {
            field = value
            requestLayout()
        }

    var space: Int = 0
        set(value) {
            field = value
            requestLayout()
        }

    private val viewInsets = Rect()

//    private val longPressTime: Int by lazy {
//        ViewConfiguration.getLongPressTimeout()
//    }

    private val contentHeight: Int
        get() {
            val tileWidth = TileLayoutHelper.tileWidth(width, spanCount, space)
            val content = tileLayoutHelper.maxLine * (tileWidth + space) + space
            return content + viewInsets.bottom
        }

    private val scrollHelper = ScrollHelper(
        this,
        {
            contentHeight - height
        },
        {
            viewInsets.top * -1
        }
    )

    private val tileLayoutHelper = TileLayoutHelper(::spanCount, { tileList.size }, ::getTileSize)

    init {
        post {
            scrollHelper.resetScrollOffset(false)
        }
    }

    private fun getTileSize(index: Int): TileSize {
        if (index < 0 || index >= tileList.size) {
            return TileSize.S
        }
        return tileList[index].size
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        tileLayoutHelper.relayout()
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val tileSpace = space
        val tileWidth = TileLayoutHelper.tileWidth(widthSize, spanCount, tileSpace)
        for (i in 0 until childCount) {
            getChildAt(i)?.let { child ->
                val block = tileLayoutHelper.getBlock(i)
                child.measure(
                    MeasureSpec.makeMeasureSpec(
                        block.width(tileWidth, tileSpace), MeasureSpec.EXACTLY
                    ),
                    MeasureSpec.makeMeasureSpec(
                        block.height(tileWidth, tileSpace), MeasureSpec.EXACTLY
                    )
                )
            }
        }
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode != MeasureSpec.EXACTLY) {
            val newHeight = tileLayoutHelper.contentHeight(tileWidth, tileSpace)
            heightSize = if (heightMode == MeasureSpec.AT_MOST) {
                min(heightSize, newHeight)
            } else {
                newHeight
            }
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val tileSpace = space
        val tileWidth = TileLayoutHelper.tileWidth(width, spanCount, tileSpace)
        val scroll = scrollHelper.scrollOffset * -1
        for (i in 0 until childCount) {
            getChildAt(i)?.let { child ->
                val block = tileLayoutHelper.getBlock(i)
                child.layout(
                    block.left(tileWidth, tileSpace),
                    block.top(tileWidth, tileSpace) + scroll,
                    block.right(tileWidth, tileSpace),
                    block.bottom(tileWidth, tileSpace) + scroll,
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return scrollHelper.onTouchEvent(event)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        scrollHelper.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    override fun scrollTo(x: Int, y: Int) {
        scrollHelper.scrollTo(x, y)
    }

    override fun scrollBy(x: Int, y: Int) {
        scrollHelper.scrollBy(x, y)
    }

    override fun computeScroll() {
        scrollHelper.computeScroll()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        scrollHelper.onScrollChanged(l, t, oldl, oldt)
    }

    fun setViewInsets(left: Int, top: Int, right: Int, bottom: Int) {
        viewInsets.set(left, top, right, bottom)
        scrollHelper.resetScrollOffset(false)
        requestLayout()
    }

    fun removeTileAt(index: Int) {
        tileList.removeAt(index)
        removeViewAt(index)
        tileLayoutHelper.notifyTileRemoved(intArrayOf(index))
    }

    fun addTile(tiles: List<Tile>) {
        for (tile in tiles) {
            addTileView(tile)
        }
        tileLayoutHelper.notifyTileAdded()
    }

    fun addTile(tile: Tile) {
        addTileView(tile)
        tileLayoutHelper.notifyTileAdded()
    }

    fun bindCreator(creator: TileCreator) {
        this.tileCreator = creator
    }

    private fun addTileView(tile: Tile) {
        val view = getTileView(tile) ?: return
        tileList.add(tile)
        addView(view)
        checkViewStatus(view)
    }

    /**
     * 磁块的View可能包含动画
     * 需要在View被展示时激活
     */
    fun onResume() {
        isActive = true
        for (i in 0 until childCount) {
            checkViewStatus(getChildAt(i))
        }
    }

    /**
     * 磁块的View可能包含动画
     * 需要在View被隐藏时暂停
     */
    fun onPause() {
        isActive = false
        for (i in 0 until childCount) {
            checkViewStatus(getChildAt(i))
        }
    }

    fun notifyTileChanged() {
        tileLayoutHelper.relayout()
        for (i in 0 until childCount) {
            getChildAt(i)?.let {
                if (it is TileView<*>) {
                    it.notifyTileChange()
                }
            }
        }
    }

    private fun checkViewStatus(view: View) {
        if (view is TileView<*>) {
            if (isActive && view.visibility == View.VISIBLE) {
                view.onResume()
            } else {
                view.onPause()
            }
        }
    }

    private fun getTileView(tile: Tile): TileView<*>? {
        return tileCreator?.createTile(tile, context)?.apply {
            bind(tile)
        }
    }

    fun interface TileCreator {
        fun createTile(tile: Tile, context: Context): TileView<*>
    }

}