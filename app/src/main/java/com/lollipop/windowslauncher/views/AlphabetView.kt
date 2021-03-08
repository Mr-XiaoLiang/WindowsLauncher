package com.lollipop.windowslauncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.AnimationHelper

/**
 * @author lollipop
 * @date 2/27/21 20:01
 * 字母索引的View
 */
class AlphabetView(
    context: Context, attributeSet: AttributeSet?, style: Int
) : ViewGroup(context, attributeSet, style) {

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null)

    /**
     * 关键字的Array
     */
    private val keyArray by lazy {
        Array(27) {
            if (it == 0) {
                "#"
            } else {
                ('A' + (it - 1)).toString()
            }
        }
    }

    /**
     * Key之间的间隔
     */
    var space: Int = 0
        set(value) {
            field = value
            requestLayout()
        }

    /**
     * 列数
     */
    var spanCount: Int = 4
        set(value) {
            field = value
            checkLayout()
        }

    /**
     * 内容高度
     */
    private var contentHeight = 0

    private var scrollOffset = 0

    private var keyStatusProvider: ((key: String, index: Int) -> Boolean)? = null

    private var keyClickListener: ((key: String, index: Int) -> Unit)? = null

    private val scrollHelper = ScrollHelper(
        this,
        { contentHeight - height },
        { paddingTop }
    )

    private val animationHelper by lazy {
        AnimationHelper(
            onUpdate = ::onAnimationUpdate,
        ).apply {
            bind(this@AlphabetView)
            onStart { view, _ ->
                if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                }
            }
            onEnd { view, _ ->
                if (progressIs(AnimationHelper.PROGRESS_MIN)) {
                    view.visibility = View.INVISIBLE
                }
            }
        }
    }

    init {
        checkLayout()
        setOnClickListener {
            close()
        }
    }

    /**
     * 为了动画考虑，增加行管理器
     * 因为有了行概念，因此需要管理行及列
     */
    private fun checkLayout() {
        val span = spanCount
        val keySize = keyArray.size
        val lineCount = (keySize / span).let {
            if (it * span < keySize) {
                it + 1
            } else {
                it
            }
        }
        while (childCount < lineCount) {
            addView(LineGroup(context, ::getKey, ::onKeyClick))
        }
        while (childCount > lineCount) {
            removeViewAt(0)
        }
        var keyViewCount = 0
        for (i in 0 until childCount) {
            getChildAt(i)?.let { lineGroup ->
                lineGroup as LineGroup
                lineGroup.spanCount = span
                lineGroup.itemSpace = space
                lineGroup.line = i
                keyViewCount += lineGroup.childCount
                while (lineGroup.childCount < span && keyViewCount < keySize) {
                    lineGroup.addView(LetterView(context))
                    keyViewCount++
                }
            }
        }
        if (keyViewCount > keySize) {
            for (i in (childCount - 1) downTo 0) {
                getChildAt(i)?.let { lineGroup ->
                    lineGroup as LineGroup
                    while (lineGroup.childCount > 0 && keyViewCount > keySize) {
                        lineGroup.removeViewAt(0)
                        keyViewCount--
                    }
                }
            }
        }
    }

    private fun getKey(index: Int): String {
        if (index < 0 || index >= keyArray.size) {
            return ""
        }
        return keyArray[index]
    }

    /**
     * 获取item的宽度信息
     * 他会去掉左右内补白，间隔宽度，再除以列数
     */
    private fun getItemWidth(widthSize: Int): Int {
        return (widthSize - paddingLeft - paddingRight - ((spanCount + 1) * space)) / spanCount
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val itemWidth = getItemWidth(widthSize)

        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY)
        for (index in 0 until childCount) {
            getChildAt(index)?.let {
                if (it is LineGroup) {
                    it.itemWidth = itemWidth
                    it.itemSpace = space
                }
                it.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val itemSpace = space
        val top = paddingTop + itemSpace
        val left = paddingLeft
        val right = width - paddingRight
        val itemWidth = getItemWidth(width)
        val scroll = scrollOffset * -1
        for (index in 0 until childCount) {
            getChildAt(index)?.let { child ->
                val childTop = index * (itemSpace + itemWidth) + top + scroll
                child.layout(left, childTop, right, childTop + itemWidth)
                val childBottom = childTop + itemWidth + itemSpace - scroll
                if (childBottom > contentHeight) {
                    contentHeight = childBottom
                }
            }
        }
        contentHeight += paddingBottom
    }

    private fun onKeyClick(index: Int) {
        if (index < 0 || index >= keyArray.size) {
            return
        }
        keyClickListener?.invoke(keyArray[index], index)
    }

    fun bindKeyStatusProvider(callback: (key: String, index: Int) -> Boolean) {
        this.keyStatusProvider = callback
    }

    fun bindKeyClickListener(callback: (key: String, index: Int) -> Unit) {
        this.keyClickListener = callback
    }

    fun updateKeyStatus() {
        val provider = keyStatusProvider ?: return
        for (index in 0 until childCount) {
            getChildAt(index)?.let { child ->
                if (child is LineGroup) {
                    child.updateKeyStatus(provider)
                }
            }
        }
    }

    fun onColorChanged(enableBg: Int, disableBg: Int, textColor: Int) {
        for (index in 0 until childCount) {
            getChildAt(index)?.let { child ->
                if (child is LineGroup) {
                    child.onColorChanged(enableBg, disableBg, textColor)
                }
            }
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        scrollHelper.resetScrollOffset(false)
        requestLayout()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val superEvent = super.onInterceptTouchEvent(ev)
        return scrollHelper.onInterceptTouchEvent(ev) || superEvent
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val superEvent = super.onTouchEvent(event)
        return scrollHelper.onTouchEvent(event) || superEvent
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        scrollHelper.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    override fun scrollTo(x: Int, y: Int) {
        scrollHelper.scrollTo(x, y)
    }

    override fun scrollBy(x: Int, y: Int) {
        scrollHelper.scrollTo(x, y)
    }

    override fun computeScroll() {
        scrollHelper.computeScroll()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        scrollHelper.onScrollChanged(l, t, oldl, oldt)
    }

    fun open() {
        val lineCount = childCount
        val lineDelay = LineGroup.DURATION / (lineCount)
        for (i in 0 until childCount) {
            getChildAt(i)?.let { child ->
                if (child is LineGroup) {
                    child.doInAnimation(i * lineDelay)
                }
            }
        }
        animationHelper.duration = lineCount * lineDelay + LineGroup.DURATION
        animationHelper.open()
    }

    fun close() {
        val lineCount = childCount
        val lineDelay = LineGroup.DURATION / (lineCount)
        for (i in 0 until childCount) {
            getChildAt(i)?.let { child ->
                if (child is LineGroup) {
                    child.doOutAnimation(i * lineDelay)
                }
            }
        }
        animationHelper.duration = lineCount * lineDelay + LineGroup.DURATION
        animationHelper.close()
    }

    private fun onAnimationUpdate(target: View, progress: Float) {
        target.background?.apply {
            alpha = (progress * 255).toInt()
            invalidateSelf()
        }
    }

    /**
     * 为了更接近WindowsPhone中的展开动画
     * 需要将Grid的布局改成Line的布局，以此来做翻转动画
     * 才能在保持列间距的同时，整体翻转
     */
    private class LineGroup(
        context: Context,
        private val keyProvider: (Int) -> String,
        private val onKeyClick: (Int) -> Unit
    ) : ViewGroup(context), OnClickListener {

        companion object {
            private const val START_IN = 1F
            private const val END_IN = 0F
            private const val START_OUT = 0F
            private const val END_OUT = -1F
            const val DURATION = 200L
        }

        var itemSpace = 0

        var spanCount = 0

        var itemWidth = 0

        var line = 0

        private val animationHelper by lazy {
            AnimationHelper(
                duration = DURATION,
                onUpdate = ::onAnimationUpdate,
            ).apply {
                bind(this@LineGroup)
            }
        }

        private fun onAnimationUpdate(target: View, progress: Float) {
            target.rotationX = progress * 90
        }

        fun doInAnimation(delay: Long) {
            animationHelper.reset(START_IN)
            onAnimationUpdate(this, START_IN)
            animationHelper.run(
                startProgress = START_IN,
                endProgress = END_IN,
                delay = delay
            )
        }

        fun doOutAnimation(delay: Long) {
            animationHelper.run(
                startProgress = START_OUT,
                endProgress = END_OUT,
                delay = delay
            )
        }

        fun updateKeyStatus(provider: (String, Int) -> Boolean) {
            for (index in 0 until childCount) {
                getChildAt(index)?.let { child ->
                    if (child is LetterView) {
                        val position = line * spanCount + index
                        child.enable = provider(keyProvider(position), position)
                    }
                }
            }
        }

        fun onColorChanged(enableBg: Int, disableBg: Int, textColor: Int) {
            for (index in 0 until childCount) {
                getChildAt(index)?.let { child ->
                    if (child is LetterView) {
                        child.onColorChanged(enableBg, disableBg, textColor)
                    }
                }
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)

            val childMeasureSpec = MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY)
            for (index in 0 until childCount) {
                getChildAt(index)?.measure(childMeasureSpec, childMeasureSpec)
            }
            setMeasuredDimension(widthSize, heightSize)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            for (index in 0 until childCount) {
                getChildAt(index)?.let { child ->
                    val childLeft = index * (itemSpace + itemWidth) + itemSpace
                    val childTop = 0
                    child.layout(childLeft, childTop, childLeft + itemWidth, childTop + itemWidth)
                    if (child is LetterView) {
                        child.bindText(keyProvider(line * spanCount + index))
                        child.setOnClickListener(this)
                    }
                }
            }
        }

        override fun onClick(v: View?) {
            v ?: return
            val index = indexOfChild(v)
            if (index < 0) {
                return
            }
            onKeyClick(line * spanCount + index)
        }

    }

    private class LetterView(context: Context) : View(context) {

        private val keyNumberDrawable = KeyNumberDrawable().apply {
            style = KeyNumberDrawable.Style.Full
            useBoldText(true)
        }

        var enable: Boolean = true
            set(value) {
                field = value
                onStatusChange()
            }

        private var enableBackground: Int = LColor.primary
        private var disableBackground: Int = LColor.primaryReversal
        private var textColor: Int = LColor.foreground

        init {
            background = keyNumberDrawable
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            keyNumberDrawable.padding = minOf(w, h) * 0.1F
            keyNumberDrawable.textSize = minOf(w, h) * 0.3F
            invalidate()
        }

        private fun onStatusChange() {
            isClickable = enable
            keyNumberDrawable.backgroundColor = if (enable) {
                enableBackground
            } else {
                disableBackground
            }
            keyNumberDrawable.foregroundColor = textColor
            invalidate()
        }

        fun onColorChanged(enableBg: Int, disableBg: Int, textColor: Int) {
            this.enableBackground = enableBg
            this.disableBackground = disableBg
            this.textColor = textColor
            invalidate()
        }

        fun bindText(key: String) {
            keyNumberDrawable.text = key
            invalidate()
        }

    }

}