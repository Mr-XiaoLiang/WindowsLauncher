package com.lollipop.windowslauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.tile.Orientation
import com.lollipop.windowslauncher.tile.TileSize

/**
 * @author lollipop
 * @date 1/31/21 19:36
 */
class TileItemShell(
    context: Context,
    attributeSet: AttributeSet?,
    style: Int
): ViewGroup(context, attributeSet, style) {

    companion object {

        private const val enable = false

        fun shellWith(view: View): View {
            if (!enable) {
                return view
            }
            val tileItemShell = TileItemShell(view.context)
            tileItemShell.addView(view)
            tileItemShell.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            return tileItemShell
        }
    }

    constructor(context: Context,
                attributeSet: AttributeSet?): this(context, attributeSet, 0)

    constructor(context: Context): this(context, null)

    private var tileSize = TileSize.M

    private var orientation = Orientation.Vertical

    /**
     * 宽度相对于高度的比例
     * w / h = ratio
     */
    private val ratio: Float
        get() {
            return tileSize.height * 1F / tileSize.width
        }

    fun setTileSize(size: TileSize) {
        if (size != this.tileSize) {
            this.tileSize = size
            requestLayout()
        }
    }

    fun setOrientation(orientation: Orientation) {
        this.orientation = orientation
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec: Int
        val heightSpec: Int
        if (orientation.isVertical) {
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY)
            heightSpec = MeasureSpec.makeMeasureSpec((widthSize * ratio).toInt(), MeasureSpec.EXACTLY)
        } else {
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            widthSpec = MeasureSpec.makeMeasureSpec((heightSize / ratio).toInt(), MeasureSpec.EXACTLY)
            heightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(widthSpec, heightSpec)
        }
        setMeasuredDimension(
            MeasureSpec.getSize(widthSpec),
            MeasureSpec.getSize(heightSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val w = right - left
        val h = bottom - top
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, w, h)
        }
    }

}