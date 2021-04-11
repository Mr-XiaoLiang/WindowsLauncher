package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/**
 * @author lollipop
 * @date 4/8/21 23:21
 * 拖拽的容器，它会在需要的时候拦截事件，
 * 并且拖拽传递事件
 * ！！
 */
class DragGroup(context: Context, attrs: AttributeSet?, style: Int) :
    FrameLayout(context, attrs, style) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    /**
     * 快照的图片本身
     */
    private var snapshotBitmap: Bitmap? = null

    /**
     * 快照的偏移量
     * 用于确定快照显示的位置（与View对应
     */
    private val snapshotOffset = Point()

    /**
     * 手指按下的位置
     */
    private val touchDownLocation = PointF()

    /**
     * 手指的焦点id（用于确定手指
     */
    private var touchActiveId = 0

    /**
     * 是否激活了拖拽
     * 拖拽开始前禁止多手指
     * 因此如果触发拖拽前已经处于多指头状态了
     * 那么拒绝触发
     */
    private var dragEnable = true

    /**
     * 拖拽的状态
     */
    private var dragState = DragState.IDLE

    fun startDrag(view: View): Boolean {
        if (!dragEnable) {
            return false
        }
        checkSnapshot(view)
        snapshotBitmap ?: return false

        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragEnable = true
                onTouchMove(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                onTouchMove(ev)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 空闲状态下，如果多指头按下了，那么拒绝开始拖拽
                if (dragState.isIdle) {
                    dragEnable = false
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // 如果不在拖拽状态，并且发现焦点手指被抬起了，
                // 那么转移焦点到最前面的手指，并且更新手指按下位置（避免滑动计算时发生跳跃
                if (!dragState.isIdle && ev.findPointerIndex(touchActiveId) < 0) {
                    touchActiveId = ev.getPointerId(0)
                    touchDownLocation.set(ev.activeX, ev.activeY)
                }
            }
            MotionEvent.ACTION_UP -> {
                onTouchUp(ev)
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun onTouchMove(ev: MotionEvent) {
        if (!dragEnable) {
            return
        }
        if (dragState.isIdle) {
            touchDownLocation.set(ev.activeX, ev.activeY)
            return
        }
        // TODO 拖拽的逻辑需要实现
    }

    private fun onTouchUp(ev: MotionEvent) {
        // TODO 抬手逻辑待实现
    }

    private val MotionEvent.activeX: Float
        get() {
            return getX(getActiveIndex())
        }

    private val MotionEvent.activeY: Float
        get() {
            return getY(getActiveIndex())
        }

    private fun MotionEvent.getActiveIndex(): Int {
        var index = findPointerIndex(touchActiveId)
        if (index < 0) {
            touchActiveId = getPointerId(0)
            index = 0
        }
        return index
    }

    private fun checkSnapshot(view: View) {
        if (!view.isShown || view.width < 1 || view.height < 1) {
            snapshotBitmap = null
            return
        }
        val oldSnapshot = snapshotBitmap
        if (oldSnapshot != null) {
            if (oldSnapshot.width != view.width
                || oldSnapshot.height != view.height
            ) {
                snapshotBitmap = null
                oldSnapshot.recycle()
            }
        }
        val newSnapshot = snapshotBitmap ?: Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(newSnapshot)
        view.draw(canvas)
        snapshotBitmap = newSnapshot
    }

    private enum class DragState {
        IDLE, DRAGGING;

        val isIdle: Boolean
            get() {
                return this == IDLE
            }
    }

}