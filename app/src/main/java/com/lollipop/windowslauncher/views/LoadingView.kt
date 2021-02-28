package com.lollipop.windowslauncher.views

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
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

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null)

    var style = Style.Line
        set(value) {
            field = value
            getLoadingDrawable()
        }

    init {
        getLoadingDrawable()
        visibility = INVISIBLE
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

    private var onHideCallback: (() -> Unit)? = null

    fun show() {
        getLoadingDrawable().start()
        visibility = VISIBLE
    }

    fun hide(callback: (() -> Unit)? = null) {
        this.onHideCallback = callback
        if (!getLoadingDrawable().isRunning) {
            onAnimationStop()
            return
        }
        getLoadingDrawable().stop()
    }

    fun defArcStyle() {
        style = Style.Arc
        pointWeight = 0.15F
        loadingWeight = 0.3F
    }

    fun defLineStyle() {
        style = Style.Line
        pointWeight = 1F
        loadingWeight = 0.2F
    }

    private fun onAnimationStop() {
        visibility = GONE
        onHideCallback?.invoke()
        onHideCallback = null
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
            newDrawable.onAnimationStop(::onAnimationStop)
            background = newDrawable
        }
        return newDrawable
    }

    enum class Style {
        Line, Arc
    }

    private abstract class LoadingDrawable :
        Drawable(),
        Animatable,
        ValueAnimator.AnimatorUpdateListener,
        Animator.AnimatorListener {

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

        protected var callStop = false
            private set

        protected var pointDiameter = 0F

        protected var pointOffset = 0F

        protected val animator by lazy {
            ValueAnimator().apply {
                addUpdateListener(this@LoadingDrawable)
                addListener(this@LoadingDrawable)
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
            }
        }

        private var onAnimationStopListener: (() -> Unit)? = null

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            bounds ?: return
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

        protected fun formatProgress(input: Float, isLimit: Boolean = true): Float {
            return LoadingAnimationHelper.formatProgress(input, isLimit)
        }

        protected fun allPoint(run: (Int) -> Unit) {
            for (i in 0 until pointCount) {
                run(i)
            }
        }

        fun onAnimationStop(callback: () -> Unit) {
            this.onAnimationStopListener = callback
        }

        override fun start() {
            callStop = false
        }

        override fun stop() {
            callStop = true
        }

        override fun isRunning(): Boolean {
            return animator.isRunning
        }

        override fun onAnimationUpdate(animation: ValueAnimator?) {
            if (animation == animator) {
                progress = animation.animatedValue as Float
            }
        }

        override fun onAnimationStart(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {}

        override fun onAnimationCancel(animation: Animator?) {}

        override fun onAnimationRepeat(animation: Animator?) {
            if (callStop) {
                animator.cancel()
                onAnimationStopListener?.invoke()
            }
        }
    }

    private class ArcLoadingDrawable : LoadingDrawable() {

        private var maxValue = 0F

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
            return (progress / maxValue) * 360
        }

        private fun checkPointProgress(value: Float): Float {
            if (value < 1) {
                return value
            }
            return value - 1
        }

        private fun getPointAngle(index: Int): Float? {
            val offset = pointOffset * index
            val pointProgress = progress - offset
            val allAngle = if (pointProgress < 1) {
                360F
            } else {
                360 * (1 - (pointOffset * (pointCount + 1.5F)))
            }
            val angle = (formatProgress(
                checkPointProgress(pointProgress), false)) * allAngle - offset * 360
            if (pointProgress < 1 && angle < 0) {
                return null
            }
            if (pointProgress > 2 && angle > 360) {
                return null
            }
            return angle + 90
        }

        override fun start() {
            super.start()
            maxValue = 2 + loadingWeight * 2
            animator.cancel()
            animator.duration = LoadingAnimationHelper.DURATION * 2
            animator.setFloatValues(0F, maxValue)
            animator.start()
        }

    }

    private class LineLoadingDrawable : LoadingDrawable() {

        private var road = 0F

        private var length = 0

        private val tempPoint = PointF()

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            bounds ?: return
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
            super.start()
            animator.cancel()
            animator.duration = LoadingAnimationHelper.DURATION
            animator.setFloatValues(0F, 1 + loadingWeight)
            animator.start()
        }

    }

    object LoadingAnimationHelper : TimeInterpolator {

        const val DURATION = 3000L

        /**
         * 通过5次函数的无极值曲线，并且添加偏移量，
         * 使得X = [0, 1]时，Y = [0, 1]，并且接近0.5时发生停顿
         * 可以换成3次函数，但是停顿时间将会变短
         */
        fun formatProgress(input: Float, isLimit: Boolean = true): Float {
            val x = if (isLimit) { input.range(0F, 1F) } else {input}
            val d = (x - 0.5F) * 2
            return (d * d * d * d * d + 1) / 2
        }

        override fun getInterpolation(input: Float): Float {
            return formatProgress(input)
        }

    }

}