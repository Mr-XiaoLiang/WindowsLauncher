package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
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

    private var snapshotBitmap: Bitmap? = null

    fun startDrag(view: View) {
        checkSnapshot(view)

    }

    private fun checkSnapshot(view: View) {
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

}