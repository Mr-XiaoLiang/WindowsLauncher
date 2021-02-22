package com.lollipop.windowslauncher.views

import android.graphics.*
import android.graphics.drawable.Drawable
import com.lollipop.windowslauncher.utils.dp2px
import com.lollipop.windowslauncher.utils.sp2px

/**
 * @author lollipop
 * @date 2/22/21 22:40
 * App列表的关键字的Drawable
 */
class KeyNumberDrawable: Drawable() {

    var backgroundColor = Color.BLACK

    var foregroundColor = Color.WHITE

    var style = Style.Stroke

    var text: String = ""

    private var padding = 0F

    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText
    }

    fun setTextSize(valueSp: Int) {
        paint.textSize = valueSp.sp2px()
    }

    fun setStrokeWidth(valueDp: Int) {
        paint.strokeWidth = valueDp.dp2px()
    }

    fun setPadding(valueDp: Int) {
        padding = valueDp.dp2px()
    }

    fun useBoldText(enable: Boolean) {
        paint.isFakeBoldText = enable
    }

    override fun draw(canvas: Canvas) {
        paint.style = style.bgStyle
        paint.color = backgroundColor
        canvas.drawRect(bounds, paint)
        if (text.isNotEmpty()) {
            paint.color = foregroundColor
            val width = paint.measureText(text)
            val textSize = paint.textSize
            val strokeWidth = paint.strokeWidth
            val fm = paint.fontMetrics
            val textY = (bounds.bottom
                    - strokeWidth
                    - padding - (textSize / 2)
                    - fm.descent
                    + (fm.descent - fm.ascent) / 2)
            val textX = bounds.left + strokeWidth + padding + (width / 2)
            canvas.drawText(text, textX, textY, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    enum class Style(val bgStyle: Paint.Style) {
        Full(Paint.Style.FILL_AND_STROKE),
        Stroke(Paint.Style.STROKE)
    }

}