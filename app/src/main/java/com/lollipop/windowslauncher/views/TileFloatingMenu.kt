package com.lollipop.windowslauncher.views

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.TileSize
import kotlin.math.min

/**
 * @author lollipop
 * @date 3/19/21 22:43
 * 瓷砖的悬浮菜单
 */
class TileFloatingMenu private constructor(private val option: Option) {

    companion object {
        /**
         * 纵向列表的菜单
         */
        fun listMenu(): Builder {
            return Builder(Style.List)
        }

        /**
         * 横向方块的菜单
         */
        fun blockMenu(): Builder {
            return Builder(Style.Block)
        }
    }

    /**
     * 根结点
     */
    private val rootGroup: ViewGroup by lazy {
        AnchorLayoutGroup(option.anchor)
    }

    /**
     * 列表的View
     */
    private val listView: RecyclerView by lazy {
        RecyclerView(option.anchor.context).apply {
            rootGroup.addView(
                this,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun show() {
        // todo
    }


    private class AnchorLayoutGroup(
        val anchor: View
    ) : FrameLayout(anchor.context) {

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            getLayoutOffset { isTop, maxHeight, baseLine ->
                for (index in 0 until childCount) {
                    getChildAt(index)?.let { child ->
                        val childHeight = min(maxHeight, child.measuredHeight)
                        if (childHeight != child.measuredHeight) {
                            // 如果尺寸不一致，那么再测量一次
                            child.measure(
                                MeasureSpec.makeMeasureSpec(child.width, MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY),
                            )
                        }
                        val offsetY = if (isTop) {
                            baseLine - childHeight - child.top
                        } else {
                            baseLine - child.top
                        }
                        ViewCompat.offsetTopAndBottom(child, offsetY)
                    }
                }
            }
        }

        private fun getLayoutOffset(
            callback: (isTop: Boolean, maxHeight: Int, baseLine: Int) -> Unit
        ) {
            val anchorLocation = anchor.getLocationInWindow()
            val myLocation = this.getLocationInWindow()

            val topSpace = anchorLocation[1] - myLocation[1]
            val bottomSpace = height - topSpace - anchor.height
            if (topSpace > bottomSpace) {
                callback(true, topSpace, topSpace)
            } else {
                callback(false, bottomSpace, height - bottomSpace)
            }
        }

        private fun View.getLocationInWindow(): IntArray {
            val location = IntArray(2)
            getLocationInWindow(location)
            return location
        }

    }

    class Builder(val style: Style) {
        private val resizeList = ArrayList<TileSize>()
        private val buttonList = ArrayList<ButtonInfo>()
        private var clickListener: ((id: Int) -> Unit)? = null

        fun addResizeType(size: TileSize): Builder {
            if (!resizeList.contains(size)) {
                resizeList.add(size)
            }
            return this
        }

        fun addButton(name: Int, id: Int): Builder {
            // 避免名字和id完全一致的场景（因为没有意义）
            if (buttonList.none { it.id == id && it.name == name }) {
                buttonList.add(ButtonInfo(name, id))
            }
            return this
        }

        fun onClick(listener: (id: Int) -> Unit): Builder {
            clickListener = listener
            return this
        }

        fun showIn(anchor: View): TileFloatingMenu {
            return TileFloatingMenu(
                Option(
                    style = style,
                    anchor = anchor,
                    resizeList = resizeList.toTypedArray(),
                    buttonList = buttonList.toTypedArray(),
                    onClickListener = clickListener ?: {}
                )
            ).apply {
                show()
            }
        }

    }

    private class Option(
        val style: Style,
        val anchor: View,
        val resizeList: Array<TileSize>,
        val buttonList: Array<ButtonInfo>,
        val onClickListener: (Int) -> Unit,
    )

    private class ButtonInfo(
        val name: Int,
        val id: Int
    )

    enum class Style {
        Block, List
    }

}