package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.lollipop.windowslauncher.utils.AnimationHelper
import com.lollipop.windowslauncher.utils.task

/**
 * @author lollipop
 * @date 1/30/21 21:56
 * 首页的指示器
 */
class HomeIndicatorView(context: Context, attributeSet: AttributeSet?, style: Int):
    View(context, attributeSet, style),
    ViewPager.OnPageChangeListener,
    ViewPager.OnAdapterChangeListener {

    constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
    constructor(context: Context): this(context, null)

    companion object {
        private const val AUTO_HIDE_DELAY = 3 * 1000L
    }

    private val indicatorDrawable = IndicatorDrawable()

    private var callHide = true

    private val animationHelper = AnimationHelper { view, progress ->
        view.translationY = view.height * (1 - progress)
    }

    private val autoHideTask = task {
        if (callHide) {
            animationHelper.close()
        } else {
            animationHelper.open()
        }
    }

    init {
        background = indicatorDrawable
        animationHelper.onStart { view, _ ->
            if (view.visibility != VISIBLE) {
                view.visibility = VISIBLE
            }
        }
        animationHelper.onEnd { view, _ ->
            if (animationHelper.progressIs(AnimationHelper.PROGRESS_MIN)
                && view.visibility != INVISIBLE) {
                view.visibility = INVISIBLE
            }
        }
        animationHelper.bind(this)
        animationHelper.close(false)
    }

    fun setColor(color: Int) {
        indicatorDrawable.color = color
    }

    fun setProgress(index: Int, progress: Float = 0F) {
        indicatorDrawable.setProgress(index, progress)
    }

    fun setCount(count: Int) {
        indicatorDrawable.indicatorCount = count
    }

    fun bindViewPager(viewPager: ViewPager) {
        viewPager.addOnPageChangeListener(this)
        viewPager.addOnAdapterChangeListener(this)
        viewPager.adapter?.let {
            setCount(it.count)
        }
        setProgress(viewPager.currentItem)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        setProgress(position, positionOffset)
    }

    override fun onPageSelected(position: Int) {
        setProgress(position)
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            autoHideTask.cancel()
            callHide = true
            autoHideTask.delay(AUTO_HIDE_DELAY)
        } else {
            autoHideTask.cancel()
            callHide = false
            autoHideTask.sync()
        }
    }

    override fun onAdapterChanged(
        viewPager: ViewPager,
        oldAdapter: PagerAdapter?,
        newAdapter: PagerAdapter?
    ) {
        newAdapter?.let {
            setCount(it.count)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        autoHideTask.cancel()
        animationHelper.destroy()
    }

    private class IndicatorDrawable: Drawable() {

        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        var color: Int
            set(value) {
                paint.color = value
                invalidateSelf()
            }
            get() {
                return paint.color
            }

        var indicatorCount: Int = 2
            set(value) {
                field = value
                updateInfo()
            }

        private var index: Int = 0

        private var progress: Float = 0F

        fun setProgress(index: Int, progress: Float) {
            this.index = index
            this.progress = progress
            updateLocation()
        }

        private var indicatorLength = 0

        private val indicatorRect = Rect()

        override fun draw(canvas: Canvas) {
            canvas.drawRect(indicatorRect, paint)
        }

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            updateInfo()
        }

        private fun updateLocation() {
            val left = (indicatorLength * index + indicatorLength * progress).toInt()
            indicatorRect.offsetTo(left, bounds.top)
            invalidateSelf()
        }

        private fun updateInfo() {
            indicatorLength = (bounds.width().toFloat() / indicatorCount + 0.5F).toInt()
            indicatorRect.set(bounds.left, bounds.top, bounds.left + indicatorLength, bounds.bottom)
            updateLocation()
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

    }

}