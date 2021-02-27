package com.lollipop.windowslauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.visibleOrGone

/**
 * @author lollipop
 * @date 2/27/21 20:01
 * 字母索引的View
 */
class AlphabetView(
    context: Context, attributeSet: AttributeSet?, style: Int
) : ViewGroup(context, attributeSet, style), View.OnClickListener {

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
            requestLayout()
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
        {
            contentHeight - height
        },
        {
            paddingTop
        }
    )

    init {
        while (childCount < keyArray.size) {
            addView(LetterView(context).apply {
                setOnClickListener(this@AlphabetView)
            })
        }
        setOnClickListener {
            close()
        }
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

        val childMeasureSpec = MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY)
        for (index in 0 until childCount) {
            getChildAt(index)?.measure(childMeasureSpec, childMeasureSpec)
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val itemSpace = space
        val top = paddingTop + itemSpace
        val left = paddingLeft + itemSpace
        val itemWidth = getItemWidth(width)
        val scroll = scrollOffset * -1
        for (index in 0 until childCount) {
            getChildAt(index)?.let { child ->
                val x = index % spanCount
                val y = index / spanCount
                val childLeft = x * (itemSpace + itemWidth) + left
                val childTop = y * (itemSpace + itemWidth) + top + scroll
                child.layout(childLeft, childTop, childLeft + itemWidth, childTop + itemWidth)
                val childBottom = childTop + itemWidth + itemSpace - scroll
                if (childBottom > contentHeight) {
                    contentHeight = childBottom
                }
                if (child is LetterView) {
                    child.bindText(keyArray[index % keyArray.size])
                }
            }
        }
        contentHeight += paddingBottom
    }

    private fun onKeyClick(index: Int) {
        if (index >= keyArray.size) {
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
        val provider = keyStatusProvider?:return
        for (index in 0 until childCount) {
            getChildAt(index)?.let { child ->
                if (child is LetterView) {
                    child.enable = provider(keyArray[index % keyArray.size], index)
                }
            }
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        scrollHelper.resetScrollOffset(false)
        requestLayout()
    }

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
        visibleOrGone(true)
    }

    fun close() {
        visibleOrGone(false)
    }

    override fun onClick(v: View?) {
        v?:return
        val index = indexOfChild(v)
        if (index < 0) {
            return
        }
        onKeyClick(index)
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