package com.lollipop.windowslauncher.tile.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.listener.BackPressedListener
import com.lollipop.windowslauncher.listener.BackPressedProvider
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
) : ViewGroup(context, attributeSet, style), TileView.TileGroup, BackPressedListener {

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

    private val contentHeight: Int
        get() {
            val tileWidth = TileLayoutHelper.tileWidth(width, spanCount, space)
            val content = tileLayoutHelper.maxLine * (tileWidth + space) + space
            return content + viewInsets.bottom
        }

    private val scrollHelper = ScrollHelper(
        this,
        { contentHeight - height },
        { viewInsets.top * -1 }
    )

    private val tileLayoutHelper = TileLayoutHelper(::spanCount, { tileList.size }, ::getTileSize)

    private var tileWidth: Int = 0

    private var floatingChild: TileView<*>? = null

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tileWidth = TileLayoutHelper.tileWidth(width, spanCount, space)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        tileLayoutHelper.relayout()
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val tileSpace = space
        tileWidth = TileLayoutHelper.tileWidth(widthSize, spanCount, tileSpace)
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
        layoutChildren()
    }

    private fun layoutChildren() {
        val tileSpace = space
        tileWidth = TileLayoutHelper.tileWidth(width, spanCount, tileSpace)
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

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val superResult = super.onInterceptTouchEvent(ev)
        return scrollHelper.onInterceptTouchEvent(ev) || superResult
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val superResult = super.onTouchEvent(event)
        return scrollHelper.onTouchEvent(event) || superResult
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val context = context
        if (context is BackPressedProvider) {
            context.addBackPressedListener(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val context = context
        if (context is BackPressedProvider) {
            context.removeBackPressedListener(this)
        }
    }

    override fun addView(child: View?) {
        super.addView(child)
        if (child is TileView<*>) {
            child.bindGroup(this)
        }
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        if (view is TileView<*>) {
            view.unbindGroup()
        }
    }

    override fun removeViewAt(index: Int) {
        getChildAt(index)?.let {
            if (it is TileView<*>) {
                it.unbindGroup()
            }
        }
        super.removeViewAt(index)
    }

    override fun notifyTileSizeChange(child: TileView<*>) {
        val index = indexOfChild(child)
        if (index < 0) {
            child.unbindGroup()
            return
        }
        // 如果是浮动状态，那么需要把它清理掉
        floatingChild?.sink(-1, -1)
        if (child == floatingChild) {
            floatingChild = null
        } else {
            sinkFocusChild()
        }
        tileLayoutHelper.lock()
        val oldSnapshot = tileLayoutHelper.getSnapshot()
        val block = tileLayoutHelper.getBlock(index)
        val oldSize = block.size
        val newSize = tileList[index].size
        var isLayoutChange = false
        if (oldSize.width < newSize.width || oldSize.height < newSize.height) {
            val pushTile = tileLayoutHelper.pushTile(block.x, block.y, newSize, index)
            if (!pushTile) {
                tileLayoutHelper.resetBlock(index, true, newSize)
                tileLayoutHelper.relayout()
                isLayoutChange = true
            }
        } else {
            tileLayoutHelper.resetBlock(index, false, newSize)
            tileLayoutHelper.removeEmptyLine()
        }
        if (!isLayoutChange) {
            block.size = newSize
            child.resizeTo(
                Rect(
                    block.left(tileWidth, space),
                    block.top(tileWidth, space),
                    block.right(tileWidth, space),
                    block.bottom(tileWidth, space)
                )
            )
            moveIfViewChanged(oldSnapshot, index)
        } else {
            layoutIfViewChanged(oldSnapshot)
            scrollHelper.smoothScrollTo(0, scrollHelper.maxScrollOffset)
        }
        tileLayoutHelper.unlock()
        tileLayoutHelper.syncSize()
    }

    override fun notifyTileRemoved(child: TileView<*>) {
        val index = indexOfChild(child)
        if (index < 0) {
            child.unbindGroup()
            return
        }
        val oldSnapshot = tileLayoutHelper.getSnapshot()
        tileLayoutHelper.notifyTileRemoved(intArrayOf(index))
        child.alpha(0F)
        moveIfViewChanged(oldSnapshot, index)
    }

    private fun moveIfViewChanged(oldSnapshot: TileLayoutHelper.Snapshot, skip: Int = -1) {
        tileLayoutHelper.diff(oldSnapshot) { i, block ->
            if (skip != i) {
                getChildAt(i)?.let {
                    if (it is TileView<*>) {
                        it.moveTo(
                            block.left(tileWidth, space),
                            block.top(tileWidth, space)
                        )
                    }
                }
            }
        }
    }

    private fun layoutIfViewChanged(oldSnapshot: TileLayoutHelper.Snapshot, skip: Int = -1) {
        val tileSpace = space
        val scroll = scrollHelper.scrollOffset * -1
        tileLayoutHelper.diff(oldSnapshot) { i, block ->
            if (skip != i) {
                getChildAt(i)?.let { child ->
                    if (child is TileView<*>) {
                        child.notifyTileChange()
                    }
                    child.measure(
                        MeasureSpec.makeMeasureSpec(
                            block.width(tileWidth, tileSpace), MeasureSpec.EXACTLY
                        ),
                        MeasureSpec.makeMeasureSpec(
                            block.height(tileWidth, tileSpace), MeasureSpec.EXACTLY
                        )
                    )
                    child.layout(
                        block.left(tileWidth, tileSpace),
                        block.top(tileWidth, tileSpace) + scroll,
                        block.right(tileWidth, tileSpace),
                        block.bottom(tileWidth, tileSpace) + scroll,
                    )
                }
            }
        }
    }

    private fun fixLayout(oldSnapshot: TileLayoutHelper.Snapshot) {
        tileLayoutHelper.diff(oldSnapshot) { i, block ->
            getChildAt(i)?.let {
                if (it is TileView<*>) {
                    it.offsetLeftAndRight(block.left(tileWidth, space) - it.left)
                    it.offsetTopAndBottom(block.top(tileWidth, space) - it.top)
                }
            }
        }
    }

    override fun requestLayoutMe(child: TileView<*>) {
        val index = indexOfChild(child)
        if (index < 0) {
            child.unbindGroup()
            return
        }
        val block = tileLayoutHelper.getBlock(index)
        val tileSpace = space
        val scroll = scrollHelper.scrollOffset * -1
        child.measure(
            MeasureSpec.makeMeasureSpec(block.width(tileWidth, space), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(block.height(tileWidth, space), MeasureSpec.EXACTLY),
        )
        child.layout(
            block.left(tileWidth, tileSpace),
            block.top(tileWidth, tileSpace) + scroll,
            block.right(tileWidth, tileSpace),
            block.bottom(tileWidth, tileSpace) + scroll,
        )
    }

    override fun onFloating(child: TileView<*>) {
        floatingChild?.sink()
        floatingChild = child
    }

    override fun onBackPressed(): Boolean {
        return sinkFocusChild()
    }

    private fun sinkFocusChild(): Boolean {
        val child = floatingChild?:return false
        floatingChild = null
        child.sink()
        return true
    }

}