package com.lollipop.windowslauncher.views

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.databinding.MenuTileFloatingBinding
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.utils.dp2px
import com.lollipop.windowslauncher.utils.withThis
import kotlin.math.min

/**
 * @author lollipop
 * @date 3/19/21 22:43
 * 瓷砖的悬浮菜单
 */
class TileFloatingMenu private constructor(private val option: Option) {

    companion object {
        fun create(): Builder {
            return Builder()
        }
    }

    /**
     * 根结点
     */
    private val rootGroup: ViewGroup by lazy {
        AnchorLayoutGroup(option.anchor)
    }

    /**
     * 气泡窗口的ViewBinding
     */
    private val popupView: MenuTileFloatingBinding by rootGroup.withThis(true)

    /**
     * 重设尺寸的按钮列表
     */
    private val resizeButtonList: RecyclerView
        get() {
            return popupView.resizeButtonList
        }

    /**
     * 菜单按钮的列表
     */
    private val menuButtonList: RecyclerView
        get() {
            return popupView.menuButtonList
        }

    private fun show() {
        // 都是空的就不显示了
        if (option.resizeList.isEmpty() && option.buttonList.isEmpty()) {
            return
        }
        // TODO
    }

    private fun initListView() {

    }

//    private class ButtonAdapter(option: Option): RecyclerView.Adapter<>

    private class ResizeButtonHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    private class MenuButtonHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    private class AnchorLayoutGroup(
        val anchor: View
    ) : FrameLayout(anchor.context) {

        private val childAnimationStyles = ArrayList<AnimationStyle>()

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            childAnimationStyles.clear()
            getLayoutOffset { isTop, maxHeight, baseLine ->
                val groupHeight = height
                val groupWidth = width
                for (index in 0 until childCount) {
                    getChildAt(index)?.let { child ->
                        val childHeight = child.measuredHeight
                        val childWidth = child.measuredWidth
                        val animationStyle = if (childHeight > maxHeight) {
                            AnimationStyle.Sheet
                        } else {
                            AnimationStyle.Expansion
                        }
                        childAnimationStyles.add(animationStyle)

                        val childTop = when (animationStyle) {
                            AnimationStyle.Expansion -> {
                                if (isTop) {
                                    baseLine - childHeight
                                } else {
                                    baseLine
                                }
                            }
                            AnimationStyle.Sheet -> {
                                groupHeight - childHeight
                            }
                        }
                        val childLeft = (groupWidth - childWidth) / 2

                        child.layout(
                            childLeft,
                            childTop,
                            childLeft + childHeight,
                            childTop + childHeight
                        )
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

    class Builder {
        private val resizeList = ArrayList<TileSize>()
        private val buttonList = ArrayList<ButtonInfo>()
        private var menuClickListener: OnMenuClickListener? = null
        private var resizeClickListener: OnResizeClickListener? = null

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

        fun onClick(listener: OnMenuClickListener): Builder {
            menuClickListener = listener
            return this
        }

        fun onResize(listener: OnResizeClickListener): Builder {
            resizeClickListener = listener
            return this
        }

        fun showIn(anchor: View): TileFloatingMenu {
            return TileFloatingMenu(
                Option(
                    anchor = anchor,
                    resizeList = resizeList.toTypedArray(),
                    buttonList = buttonList.toTypedArray(),
                    onMenuClickListener = menuClickListener ?: OnMenuClickListener {},
                    onResizeClickListener = resizeClickListener ?: OnResizeClickListener {}
                )
            ).apply {
                show()
            }
        }

    }

    /**
     * 参数配置信息
     * 它是不可变的
     */
    private class Option(
        /**
         * 菜单关联的锚点
         */
        val anchor: View,

        /**
         * 重置大小的按钮列表
         */
        val resizeList: Array<TileSize>,

        /**
         * 菜单按钮列表
         */
        val buttonList: Array<ButtonInfo>,

        /**
         * 普通菜单按钮点击事件的监听器
         */
        val onMenuClickListener: OnMenuClickListener,

        /**
         * 尺寸重置按钮点击的监听器
         */
        val onResizeClickListener: OnResizeClickListener
    )

    /**
     * 按钮当描述信息
     */
    private class ButtonInfo(
        /**
         * 按钮名称
         */
        @StringRes
        val name: Int,

        /**
         * 按钮的ID
         */
        val id: Int
    )

    /**
     * 动画类型
     */
    private enum class AnimationStyle {

        /**
         * 展开模式
         * 当高度允许的时候，在锚点附近展开
         */
        Expansion,

        /**
         * 底部抽屉模式
         * 当锚点附近无法完整展开菜单时
         * 在底部放置菜单布局
         */
        Sheet
    }

    /**
     * 菜单被点击时的监听器
     */
    fun interface OnMenuClickListener {
        /**
         * 当菜单被点击时
         * @param id 被点击按钮的对应id
         */
        fun onMenuClick(id: Int)
    }

    /**
     * 尺寸重置按钮被点击时的监听器
     */
    fun interface OnResizeClickListener {

        /**
         * 尺寸重置按钮被点击时
         * @param newSize 被点击的尺寸信息
         */
        fun onResizeClick(newSize: TileSize)
    }

}