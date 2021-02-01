package com.lollipop.windowslauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.lollipop.windowslauncher.tile.TileSize

/**
 * @author lollipop
 * @date 1/31/21 19:36
 */
class TileItemShell(
    context: Context,
    attributeSet: AttributeSet?,
    style: Int
): FrameLayout(context, attributeSet, style) {

    companion object {
        fun shellWith(view: View): TileItemShell {
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((widthSize * ratio).toInt(), MeasureSpec.EXACTLY),
        )
    }

}