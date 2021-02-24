package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * @author lollipop
 * @date 2/24/21 22:52
 * 椭圆形的图标按钮
 * 他不是透明的，他有背景色
 */
class OvalIconButton(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int
) : AppCompatImageView(context, attrs, defStyle) {

    constructor(context: Context, attr: AttributeSet?): this(context, attr, 0)
    constructor(context: Context): this(context, null)

    override fun setBackgroundColor(color: Int) {
        val backgroundDrawable = background
        if (backgroundDrawable is OvalBackground) {
            (backgroundDrawable.mutate() as OvalBackground).color = color
        } else {
            background = OvalBackground(color)
        }
    }

    override fun setBackground(background: Drawable?) {
        if (background is ColorDrawable && background !is OvalBackground) {
            super.setBackground(OvalBackground(background.color))
            return
        }
        super.setBackground(background)
    }

    private class OvalBackground(color: Int): ColorDrawable(color) {

        private val clipPath = Path()

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            clipPath.reset()
            bounds?:return
            clipPath.addOval(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat(),
                Path.Direction.CW
            )
        }

        override fun draw(canvas: Canvas) {
            val saveCount = canvas.save()
            canvas.clipPath(clipPath)
            super.draw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }

}