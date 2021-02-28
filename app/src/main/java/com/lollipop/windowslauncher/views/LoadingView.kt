package com.lollipop.windowslauncher.views

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.lollipop.windowslauncher.utils.range
import kotlin.math.max
import kotlin.math.min

/**
 * @author lollipop
 * @date 2/28/21 02:48
 * 模仿Windows的加载动画
 */
class LoadingView(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int
) : View(context, attrs, defStyle) {

    constructor(context: Context, attr: AttributeSet?): this(context, attr, 0)
    constructor(context: Context): this(context, null)

    var style = Style.Line
        set(value) {
            field = value
            getLoadingDrawable()
        }

    init {
        getLoadingDrawable()
    }

    var pointColor: Int
        get() {
            return getLoadingDrawable().pointColor
        }
        set(value) {
            getLoadingDrawable().pointColor = value
            invalidate()
        }

    var pointWeight: Float
        get() {
            return getLoadingDrawable().pointWeight
        }
        set(value) {
            getLoadingDrawable().pointWeight = value
            invalidate()
        }

    var pointCount: Int
        get() {
            return getLoadingDrawable().pointCount
        }
        set(value) {
            getLoadingDrawable().pointCount = value
            invalidate()
        }

    var loadingWeight: Float
        get() {
            return getLoadingDrawable().loadingWeight
        }
        set(value) {
            getLoadingDrawable().loadingWeight = value
            invalidate()
        }

    fun start() {
        getLoadingDrawable().start()
    }

    private fun getLoadingDrawable(): LoadingDrawable {
        val oldDrawable = background
        val newDrawable = when (style) {
            Style.Line -> {
                if (oldDrawable is LineLoadingDrawable) {
                    oldDrawable
                } else {
                    LineLoadingDrawable()
                }
            }
            Style.Arc -> {
                if (oldDrawable is ArcLoadingDrawable) {
                    oldDrawable
                } else {
                    ArcLoadingDrawable()
                }
            }
        }
        if (oldDrawable != newDrawable) {
            if (oldDrawable is LoadingDrawable) {
                newDrawable.pointColor = oldDrawable.pointColor
                newDrawable.pointCount = oldDrawable.pointCount
                newDrawable.pointWeight = oldDrawable.pointWeight
                newDrawable.loadingWeight = oldDrawable.loadingWeight
                if (oldDrawable.isRunning) {
                    newDrawable.start()
                }
            }
            background = newDrawable
        }
        return newDrawable
    }

    enum class Style {
        Line, Arc
    }

    private abstract class LoadingDrawable:
        Drawable(), Animatable, ValueAnimator.AnimatorUpdateListener {

        protected val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE
        }

        var pointColor: Int
            get() {
                return paint.color
            }
            set(value) {
                paint.color = value
            }

        var pointWeight = 1F

        var loadingWeight = 0.2F

        var pointCount = 5
            set(value) {
                field = value
                onBoundsChange(bounds)
            }

        var progress: Float = 0F
            set(value) {
                field = value
                invalidateSelf()
            }

        val pointRadius: Float
            get() {
                return pointDiameter / 2
            }

        protected var pointDiameter = 0F

        protected var pointOffset = 0F

        protected val animator by lazy {
            ValueAnimator().apply {
                addUpdateListener(this@LoadingDrawable)
                repeatCount = ValueAnimator.INFINITE
            }
        }

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            bounds?:return
            pointDiameter = min(bounds.width(), bounds.height()) * pointWeight
            pointOffset = loadingWeight / (pointCount - 1)
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

        protected fun formatProgress(input: Float): Float {
            return LoadingAnimationHelper.getInterpolation(input)
        }

        protected fun allPoint(run: (Int) -> Unit) {
            for (i in 0 until pointCount) {
                run(i)
            }
        }

        override fun stop() {
            animator.cancel()
        }

        override fun isRunning(): Boolean {
            return animator.isRunning
        }

        override fun onAnimationUpdate(animation: ValueAnimator?) {
            if (animation == animator) {
                progress = animation.animatedValue as Float
            }
        }

    }

    private class ArcLoadingDrawable: LoadingDrawable() {

        override fun draw(canvas: Canvas) {
            val radius = pointRadius
            val x = bounds.width() / 2 - radius
            allPoint { i ->
                getPointAngle(i)?.let { angle ->
                    canvas.save()
                    canvas.translate(bounds.exactCenterX(), bounds.exactCenterY())
                    canvas.rotate(angle + getContentAngle())
                    canvas.drawCircle(x, 0F, radius, paint)
                    canvas.restore()
                }
            }
        }

        private fun getContentAngle(): Float {
            return progress * 360
        }

        private fun getPointAngle(index: Int): Float? {
            val offset = pointOffset * index
            val pointProgress = progress - offset
            // 模拟出现动作
            if (pointProgress < 0) {
                return null
            }
            // 模拟收起动作
            if (pointProgress > 2) {
                return null
            }
            return (formatProgress(
                pointProgress - pointProgress.toInt()) - offset) * 360 + 90
        }

        override fun start() {
            animator.cancel()
            animator.duration = LoadingAnimationHelper.DURATION * 3
            animator.setFloatValues(0F, 2 + (pointWeight * 3))
            animator.start()
        }

    }

    private class LineLoadingDrawable: LoadingDrawable() {

        private var road = 0F

        private var length = 0

        private val tempPoint = PointF()

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            bounds?:return
            length = max(bounds.width(), bounds.height())
            road = length * (1 + loadingWeight) + pointDiameter
        }

        override fun draw(canvas: Canvas) {
            val radius = pointRadius
            allPoint { i ->
                val pointLocation = getPointLocation(i)
                canvas.drawCircle(pointLocation.x, pointLocation.y, radius, paint)
            }
        }

        private fun getPointLocation(index: Int): PointF {
            val pointProgress = formatProgress(progress - (index * pointOffset))
            val offset = length * pointOffset * index
            val length = pointProgress * road - (pointDiameter / 2) - offset
            if (bounds.width() < bounds.height()) {
                tempPoint.set(bounds.exactCenterX(), bounds.top + length)
            } else {
                tempPoint.set(bounds.left + length, bounds.exactCenterY())
            }
            return tempPoint
        }

        override fun start() {
            animator.cancel()
            animator.duration = LoadingAnimationHelper.DURATION
            animator.setFloatValues(0F, 1 + loadingWeight)
            animator.start()
        }

    }

    object LoadingAnimationHelper: TimeInterpolator{

        const val DURATION = 3000L

        /**
         * 通过5次函数的无极值曲线，并且添加偏移量，
         * 使得X = [0, 1]时，Y = [0, 1]，并且接近0.5时发生停顿
         * 可以换成3次函数，但是停顿时间将会变短
         */
        private fun formatProgress(x: Float): Float {
            val d = (x - 0.5F) * 2
            return (d * d * d * d * d + 1) / 2
        }

        override fun getInterpolation(input: Float): Float {
            return formatProgress(input.range(0F, 1F))
        }

    }

}