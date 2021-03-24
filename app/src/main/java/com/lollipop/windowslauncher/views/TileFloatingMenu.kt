package com.lollipop.windowslauncher.views

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
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
        // todo
    }


    private fun initListView() {

    }

//    private class ButtonAdapter(option: Option): RecyclerView.Adapter<>

    private class BlockHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    private class ListHolder(view: View) : RecyclerView.ViewHolder(view) {

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
                            childTop + childHeight)
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
        val anchor: View,
        val resizeList: Array<TileSize>,
        val buttonList: Array<ButtonInfo>,
        val onClickListener: (Int) -> Unit,
    )

    private class ButtonInfo(
        val name: Int,
        val id: Int
    )

    private enum class AnimationStyle {
        Expansion, Sheet
    }

}