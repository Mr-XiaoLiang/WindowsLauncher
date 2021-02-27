package com.lollipop.windowslauncher.views

import android.graphics.PointF
import android.view.*
import android.widget.OverScroller
import kotlin.math.abs

/**
 * @author lollipop
 * @date 2/27/21 21:56
 * 通用的滑动辅助套装，包含了一个ViewGroup的简单滑动操作
 * 它只包含了纵向的滑动内容
 * 使用时，需要重写View中相应的方法,然后调用它的同名方法以此触发滑动事件
 * 必须要重写的方法有：
 * override fun onTouchEvent(event: MotionEvent?): Boolean {
 *     return scrollHelper.onTouchEvent(event)
 * }
 *
 * override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
 *     scrollHelper.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
 * }
 *
 * override fun scrollTo(x: Int, y: Int) {
 *     scrollHelper.scrollTo(x, y)
 * }
 *
 * override fun scrollBy(x: Int, y: Int) {
 *     scrollHelper.scrollTo(x, y)
 * }
 *
 * override fun computeScroll() {
 *     scrollHelper.computeScroll()
 * }
 *
 * override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
 *     scrollHelper.onScrollChanged(l, t, oldl, oldt)
 * }
 */
class ScrollHelper(
    private val target: ViewGroup,
    private val maxScrollOffsetProvider: () -> Int,
    private val minScrollOffsetProvider: () -> Int
) {

    private val maxScrollOffset: Int
        get() {
            return maxScrollOffsetProvider()
        }

    private val minScrollOffset: Int
        get() {
            return minScrollOffsetProvider()
        }

    private var activeTouchId = 0

    var scrollOffset = minScrollOffset
        private set

    private val lastTouchLocation = PointF()

    private val scroller: OverScroller by lazy {
        OverScroller(target.context)
    }

    private var velocityTracker: VelocityTracker? = null

    /**
     * 滑动前的手指浮动区域
     */
    private val scrollTouchSlop: Int by lazy {
        ViewConfiguration.get(target.context).scaledTouchSlop;
    }

    private val minVelocity: Int by lazy {
        ViewConfiguration.get(target.context).scaledMinimumFlingVelocity
    }

    private val maxVelocity: Int by lazy {
        ViewConfiguration.get(target.context).scaledMaximumFlingVelocity
    }

    private var touchMode = TouchMode.None

    private val childCount: Int
        get() {
            return target.childCount
        }

    fun resetScrollOffset(callScroll: Boolean = true) {
        if (callScroll) {
            onScrollChanged(0, minScrollOffset, 0, scrollOffset)
        } else {
            scrollOffset = minScrollOffset
        }
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
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

    fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
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

    fun scrollTo(x: Int, y: Int) {
        onScrollChanged(0, y, 0, scrollOffset)
    }

    fun scrollBy(x: Int, y: Int) {
        scrollTo(x, y - scrollOffset)
    }

    fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            onScrollChanged(0, scroller.currY, 0, scrollOffset)
            postInvalidateOnAnimation()
        }
    }

    fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
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
            target.getChildAt(i)?.offsetTopAndBottom(newOffset * -1)
        }
    }

    private fun onTouchMove(newX: Float, newY: Float) {
        val dy = (newY - lastTouchLocation.y + 0.5F).toInt()
        if (!touchMode.isScroll) {
            if (abs(dy) > scrollTouchSlop) {
                touchMode = TouchMode.Scroll
                lastTouchLocation.set(newX, newY)
                target.parent?.requestDisallowInterceptTouchEvent(true)
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
    }

    private fun onTouchCancel(newX: Float, newY: Float) {
        dragEnd()
    }

    private fun postInvalidateOnAnimation() {
        target.postInvalidateOnAnimation()
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

    private enum class TouchMode {
        None,
        Scroll;

        val isScroll: Boolean
            get() {
                return this == Scroll
            }
    }

}