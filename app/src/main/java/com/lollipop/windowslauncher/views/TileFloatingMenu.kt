package com.lollipop.windowslauncher.views

import android.view.View
import com.lollipop.windowslauncher.tile.TileSize

/**
 * @author lollipop
 * @date 3/19/21 22:43
 * 瓷砖的悬浮菜单
 */
class TileFloatingMenu private constructor(private val option: Option) {

    private fun show() {
        // todo
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