package com.lollipop.windowslauncher.tile.view

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.tile.Tile

/**
 * @author lollipop
 * @date 2/15/21 16:40
 * 磁块展示的View
 */
abstract class TileView<T : Tile>(context: Context) : ViewGroup(context) {

    /**
     * 磁块的View辅助工具
     */
    private val tileViewHelper by lazy {
        TileViewHelper(this)
    }

    private var pendingBind = false

    private var tileGroup: TileGroup? = null

    fun bindGroup(group: TileGroup) {
        this.tileGroup = group
    }

    fun unbindGroup() {
        this.tileGroup = null
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
    fun bind(tile: Tile) {
        this.setBackgroundColor(LColor.tileBackground)
        tileViewHelper.onBind(tile)
        bindTileInfo(tile)
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindTileInfo(tile: Tile?) {
        try {
            if (tile != null) {
                if (!isAttachedToWindow) {
                    pendingBind = true
                    return
                }
                onBind(tile as T)
            }
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
        if (pendingBind) {
            pendingBind = false
            notifyTileChange()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tileViewHelper.onDetached()
    }

    fun notifyTileChange() {
        tileViewHelper.notifyTileChange()
    }

    fun callLayoutTile() {
        scaleX = 1F
        scaleY = 1F
        translationX = 0F
        translationY = 0F
        translationZ = 0F
        rotationX = 0F
        rotationY = 0F

        tileGroup?.requestLayoutMe(this)
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
            getChildAt(i)?.layout(0, 0, w, h)
        }
    }

    protected fun TextView.setShadow(color: Int) {
        setShadowLayer(2F, 1F, 1F, color)
    }

    interface TileGroup {

        fun notifyTileSizeChange(child: TileView<*>)

        fun notifyTileRemoved(child: TileView<*>)

        fun requestLayoutMe(child: TileView<*>)

    }
}