package com.lollipop.windowslauncher.tile

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.holder.AppTileHolder
import com.lollipop.windowslauncher.utils.bind
import com.lollipop.windowslauncher.utils.log

/**
 * @author lollipop
 * @date 1/31/21 20:08
 */
class TileAdapter(
    private val tileList: ArrayList<Tile>,
    private val onTileClick: (Tile) -> Unit,
    private val onTileLongClick: (Tile) -> Unit,
    ): RecyclerView.Adapter<TileHolder<*>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileHolder<*> {
        log("onCreateViewHolder: ${TileType.values()[viewType]}")
        return when (TileType.values()[viewType]) {
            TileType.App -> AppTileHolder(parent.bind(), ::onHolderClick, ::onHolderLongClick)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return tileList[position].tileType.ordinal
    }

    override fun onBindViewHolder(holder: TileHolder<*>, position: Int) {
        log("onCreateViewHolder: $holder")
        when (holder) {
            is AppTileHolder -> holder.tryBind(tileList[position])
        }
    }

    override fun getItemCount(): Int {
        return tileList.size
    }

    private fun onHolderClick(holder: TileHolder<*>) {
        onTileClick(tileList[holder.adapterPosition])
    }

    private fun onHolderLongClick(holder: TileHolder<*>) {
        onTileLongClick(tileList[holder.adapterPosition])
    }

    private inline fun <reified T: Tile> TileHolder<T>.tryBind(tile: Tile) {
        if (tile is T) {
            bind(tile)
        }
    }

}