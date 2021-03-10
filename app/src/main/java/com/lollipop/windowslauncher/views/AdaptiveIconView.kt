package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.lollipop.windowslauncher.utils.versionThen
import kotlin.math.max
import kotlin.math.min

open class AdaptiveIconView(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int
) : AppCompatImageView(context, attrs, defStyle) {

    private var iconLeft = 0
    private var iconTop = 0
    private var iconWidth = 0

    private var iconWeight = 0.8F

    private var iconWeightDef = 0.8F

    private val iconClipPath = Path()

    var adaptiveIconOutlineProvider: IconOutlineProvider? = null

    var defaultIconOutlineProvider: IconOutlineProvider? = null

    private var isAdaptiveIcon = false

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null)

    fun setIconWeight(adaptiveWeight: Float, defWeight: Float) {
        iconWeight = adaptiveWeight
        iconWeightDef = defWeight
        updateIconSize()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateIconSize()
        invalidate()
    }

    private fun updateIconSize() {
        val weight = if (versionThen(Build.VERSION_CODES.O)
            && drawable is AdaptiveIconDrawable
        ) {
            isAdaptiveIcon = true
            iconWeight
        } else {
            isAdaptiveIcon = false
            iconWeightDef
        }
        iconWidth = min(width * weight, height * weight).toInt()
        iconLeft = (width - iconWidth) / 2
        iconTop = (height - iconWidth) / 2
        iconClipPath.reset()
        if (isAdaptiveIcon) {
            adaptiveIconOutlineProvider?.getIconOutline(iconClipPath, iconLeft, iconTop, iconWidth)
        } else {
            defaultIconOutlineProvider?.getIconOutline(iconClipPath, iconLeft, iconTop, iconWidth)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val icon = drawable
        if (versionThen(Build.VERSION_CODES.O) && icon is AdaptiveIconDrawable) {
            if (!isAdaptiveIcon) {
                updateIconSize()
            }
            canvas.run {
                val saveCount = save()
                // 绘制背景
                drawBackground(this, icon.background)
                // 绘制前景
                drawForeground(this, icon.foreground, true)
                restoreToCount(saveCount)
            }
        } else {
            if (isAdaptiveIcon) {
                updateIconSize()
            }
            background?.let {
                drawBackground(canvas, it)
            }
            icon?.let {
                drawForeground(canvas, it, false)
            }
        }
    }

    private fun drawBackground(canvas: Canvas, drawable: Drawable) {
        var selfWidth = drawable.intrinsicWidth
        var selfHeight = drawable.intrinsicHeight
        if (selfWidth < 0) {
            selfWidth = width
        }
        if (selfHeight < 0) {
            selfHeight = height
        }
        val scale = max(width * 1F / selfWidth, height * 1F / selfHeight)
        val offsetX = (selfWidth * scale - width) * 0.5F
        val offsetY = (selfHeight * scale - height) * 0.5F
        canvas.save()
        canvas.translate(offsetX * -1, offsetY * -1)
        canvas.scale(scale, scale)
        drawable.setBounds(0, 0, selfWidth, selfHeight)
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun drawForeground(canvas: Canvas, drawable: Drawable, isClip: Boolean) {
        var selfWidth = drawable.intrinsicWidth
        var selfHeight = drawable.intrinsicHeight
        if (selfWidth < 0) {
            selfWidth = iconWidth
        }
        if (selfHeight < 0) {
            selfHeight = iconWidth
        }
        val scale = max(iconWidth * 1F / selfWidth, iconWidth * 1F / selfHeight)
        val offsetX = (iconWidth - selfWidth * scale) * 0.5F + iconLeft
        val offsetY = (iconWidth - selfHeight * scale) * 0.5F + iconTop
        canvas.save()
        if (isClip && !iconClipPath.isEmpty) {
            canvas.clipPath(iconClipPath)
        }
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        drawable.setBounds(0, 0, selfWidth, selfHeight)
        drawable.draw(canvas)
        canvas.restore()
    }

    fun interface IconOutlineProvider {
        fun getIconOutline(path: Path, left: Int, top: Int, iconWidth: Int)
    }

}