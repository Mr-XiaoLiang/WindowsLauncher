package com.lollipop.windowslauncher.tile.view

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.utils.log

/**
 * @author lollipop
 * @date 2/15/21 16:40
 * 磁块展示的View
 */
abstract class TileView<T : Tile>(context: Context) :
    ViewGroup(context),
    View.OnLongClickListener,
    View.OnClickListener {

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
//        setOnLongClickListener(this)
        setOnClickListener(this)
    }

    fun unbindGroup() {
        this.tileGroup = null
        setOnLongClickListener(null)
        setOnClickListener(null)
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

    /**
     * 重设大小
     */
    open fun resizeTo(bounds: Rect, delay: Long = 0) {
        tileViewHelper.resize(bounds, delay)
    }

    /**
     * 浮起
     */
    open fun float(delay: Long = 0) {
        tileViewHelper.float(delay)
        tileGroup?.onFloating(this)
    }

    /**
     * 降下
     */
    open fun sink(delay: Long = 0) {
        tileViewHelper.sink(delay)
    }

    /**
     * 透明度变化
     */
    open fun alpha(alpha: Float, delay: Long = 0) {
        tileViewHelper.alpha(alpha, delay)
    }

    override fun onLongClick(v: View?): Boolean {
        if (v == this) {
            float()
            return true
        }
        return false
    }

    override fun onClick(v: View?) {
        if (v == this) {
            onClickTile()
        }
    }

    open fun onClickTile() {
        // TODO 测试代码
        tileViewHelper.myTile?.let { tile ->
            val oldSize = tile.size
            if (tile.size == TileSize.S) {
                tile.size = TileSize.XL
            } else {
                tile.size = TileSize.S
            }
            if (oldSize != tile.size) {
                notifyTileSizeChange()
            }
        }
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

    fun notifyTileSizeChange() {
        tileGroup?.notifyTileSizeChange(this)
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
        notifyTileChange()
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

        fun onFloating(child: TileView<*>)

    }
}