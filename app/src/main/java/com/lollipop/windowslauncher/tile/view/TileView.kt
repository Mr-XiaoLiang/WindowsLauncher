package com.lollipop.windowslauncher.tile.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.tile.Tile

/**
 * @author lollipop
 * @date 2/15/21 16:40
 * 磁块展示的View
 */
abstract class TileView<T : Tile>(context: Context) : ViewGroup(context) {

    /**
     * 磁块布局的id
     */
    abstract val tileLayoutId: Int

    /**
     * 磁块的View辅助工具
     */
    private val tileViewHelper by lazy {
        TileViewHelper(this)
    }

    init {
        initTileShell()
    }

    private fun initTileShell() {
        if (tileLayoutId != 0) {
            LayoutInflater.from(context).inflate(tileLayoutId, this)
        }
    }

    /**
     * 磁块的View可能包含动画
     * 需要在View被展示时激活
     */
    open fun onResume() {
        tileViewHelper.onResume()
    }

    /**
     * 磁块的View可能包含动画
     * 需要在View被隐藏时暂停
     */
    open fun onPause() {
        tileViewHelper.onPause()
    }

    /**
     * 绑定tile数据
     */
    @Suppress("UNCHECKED_CAST")
    fun bind(tile: Tile) {
        this.setBackgroundColor(LColor.tileBackground)
        tileViewHelper.onBind(tile)
        try {
            onBind(tile as T)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 绑定tile数据
     */
    abstract fun onBind(tile: T)

    /**
     * 移动至
     */
    open fun moveTo(x: Int, y: Int, delay: Long = 0) {
        tileViewHelper.moveTo(x, y, delay)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        tileViewHelper.onAttached()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tileViewHelper.onDetached()
    }

    fun notifyTileChange() {
        tileViewHelper.notifyTileChange()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxWidth = 0
        var maxHeight = 0
        for (i in 0 until childCount) {
            getChildAt(i)?.let {
                it.measure(widthMeasureSpec, heightMeasureSpec)
                if (it.measuredWidth > maxWidth) {
                    maxWidth = it.measuredWidth
                }
                if (it.measuredHeight > maxHeight) {
                    maxHeight = it.measuredHeight
                }
            }
        }
        setMeasuredDimension(maxWidth, maxHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = width
        val h = height
        for (i in 0 until childCount) {
            getChildAt(i)?.let {
                it.layout(0, 0, w, h)
            }
        }
    }
}