package com.lollipop.windowslauncher.tile

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.views.TileItemShell

/**
 * @author lollipop
 * @date 1/31/21 16:25
 */
abstract class TileHolder<T: Tile>(
    view: View,
    private val onClick: (TileHolder<*>) -> Unit,
    private val onLongClick: (TileHolder<*>) -> Unit,
): RecyclerView.ViewHolder(TileItemShell.shellWith(view)) {

    init {
        itemView.setOnClickListener(::onClick)
        itemView.setOnLongClickListener {
            onLongClick(it)
            true
        }
    }

    open val canDrag: Boolean = true

    fun bind(tile: T) {
        if (itemView is TileItemShell) {
            itemView.setTileSize(tile.size)
        }
        itemView.setBackgroundColor(LColor.tileBackground)
        onBind(tile)
    }

    abstract fun onBind(tile: T)

    open fun onClick(view: View) {
        if (view == itemView) {
            onClick(this)
        }
    }

    open fun onLongClick(view: View) {
        if (view == itemView) {
            onLongClick(this)
        }
    }

}