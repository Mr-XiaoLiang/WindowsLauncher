package com.lollipop.windowslauncher.tile.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import android.widget.OverScroller
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import kotlin.math.abs
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

    val viewInsets = Rect()

    private val longPressTime: Int by lazy {
        ViewConfiguration.getLongPressTimeout()
    }

    private var scrollOffset = 0

    private val contentHeight: Int
        get() {
            val tileWidth = TileLayoutHelper.tileWidth(width, spanCount, space)
            val content = tileLayoutHelper.maxLine * (tileWidth + space) + space
            return content + viewInsets.top + viewInsets.bottom
        }

    private val maxScrollOffset: Int
        get() {
            return contentHeight - height
        }

    private val minScrollOffset: Int
        get() {
            return viewInsets.top
        }

    private var activeTouchId = 0

    private val lastTouchLocation = PointF()

    private val tileLayoutHelper = TileLayoutHelper(::spanCount, { tileList.size }, ::getTileSize)

    private val scroller: OverScroller by lazy {
        OverScroller(context)
    }

    private var velocityTracker: VelocityTracker? = null

    /**
     * 滑动前的手指浮动区域
     */
    private val scrollTouchSlop: Int by lazy {
        ViewConfiguration.get(context).scaledTouchSlop;
    }

    private val minVelocity: Int by lazy {
        ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    private val maxVelocity: Int by lazy {
        ViewConfiguration.get(context).scaledMaximumFlingVelocity
    }

    private var touchMode = TouchMode.None

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
        val scroll = scrollOffset * -1
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
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeTouchId = event.getPointerId(0)
                lastTouchLocation.set(event.getActiveX(), event.getActiveY())
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                    scroller.computeScrollOffset()
                    touchMode = TouchMode.Scroll
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode.isScroll) {
                    getVelocityTracker().addMovement(event)
                }
                onTouchMove(event.getActiveX(), event.getActiveY())
            }
            MotionEvent.ACTION_UP -> {
                onTouchUp(event.getActiveX(), event.getActiveY())
            }
            MotionEvent.ACTION_CANCEL -> {
                onTouchCancel(event.getActiveX(), event.getActiveY())
            }
            MotionEvent.ACTION_POINTER_UP -> {
                checkActiveTouch(event) {
                    lastTouchLocation.set(event.getActiveX(), event.getActiveY())
                    getVelocityTracker().clear()
                }
            }
        }
        return true
    }

    private fun onTouchMove(newX: Float, newY: Float) {
        val dy = (newY - lastTouchLocation.y + 0.5F).toInt()
        if (!touchMode.isScroll) {
            if (abs(dy) > scrollTouchSlop) {
                touchMode = TouchMode.Scroll
                lastTouchLocation.set(newX, newY)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            return
        }
        lastTouchLocation.set(newX, newY)
        val y = scrollOffset - dy
        onScrollChanged(0, y, 0, scrollOffset)
    }

    private fun onTouchUp(newX: Float, newY: Float) {
        if (touchMode.isScroll) {
            val tracker = getVelocityTracker()
            tracker.computeCurrentVelocity(1000, maxVelocity.toFloat())
            val initialVelocity = tracker.getYVelocity(activeTouchId).toInt()
            if (abs(initialVelocity) > minVelocity) {
                val velocityY = initialVelocity * -1
                if ((velocityY < 0 && scrollOffset > minScrollOffset)
                    || (velocityY > 0 && scrollOffset < maxScrollOffset)) {
                    scroller.fling(
                        0, scrollOffset, 0, velocityY, 0, 0, minScrollOffset,
                        maxScrollOffset, 0, 0
                    )
                    postInvalidateOnAnimation()
                }
            } else if (scroller.springBack(
                    0, scrollOffset, 0, 0, minScrollOffset, maxScrollOffset
                )) {
                postInvalidateOnAnimation()
            }
        }
        dragEnd()
        // TODO
    }

    private fun onTouchCancel(newX: Float, newY: Float) {
        dragEnd()
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        if (!scroller.isFinished) {
            onScrollChanged(0, scrollY, 0, scrollOffset)
            if (clampedY) {
                scroller.springBack(0, scrollOffset, 0, 0, 0, maxScrollOffset)
            }
            postInvalidateOnAnimation()
        } else {
            scrollTo(scrollX, scrollY)
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        onScrollChanged(0, y, 0, scrollOffset)
    }

    override fun scrollBy(x: Int, y: Int) {
        scrollTo(x, y - scrollOffset)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            onScrollChanged(0, scroller.currY, 0, scrollOffset)
            postInvalidateOnAnimation()
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        val maxOffset = maxScrollOffset
        val minOffset = minScrollOffset
        scrollOffset = t
        if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset
        }
        if (scrollOffset < minOffset) {
            scrollOffset = minOffset
        }
        val newOffset = scrollOffset - oldt
        for (i in 0 until childCount) {
            getChildAt(i)?.offsetTopAndBottom(newOffset * -1)
        }
    }

    private fun dragEnd() {
        touchMode = TouchMode.None
        recycleVelocityTracker()
    }

    private fun getVelocityTracker(): VelocityTracker {
        val tracker = velocityTracker
        if (tracker != null) {
            return tracker
        }
        val newTracker = VelocityTracker.obtain()
        velocityTracker = newTracker
        return newTracker
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun MotionEvent.getActiveX(): Float {
        return getX(checkActiveTouch(this))
    }

    private fun MotionEvent.getActiveY(): Float {
        return getY(checkActiveTouch(this))
    }

    private fun checkActiveTouch(
        motionEvent: MotionEvent,
        onChanged: (() -> Unit)? = null
    ): Int {
        var index = motionEvent.findPointerIndex(activeTouchId)
        if (index < 0) {
            index = 0
            activeTouchId = motionEvent.getPointerId(index)
            onChanged?.invoke()
        }
        return index
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

    private enum class TouchMode {
        None,
        Scroll;

        val isScroll: Boolean
            get() {
                return this == Scroll
            }
    }

    fun interface TileCreator {
        fun createTile(tile: Tile, context: Context): TileView<*>
    }

}