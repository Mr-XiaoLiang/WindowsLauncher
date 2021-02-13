package com.lollipop.windowslauncher.fragment

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.base.BaseFragment
import com.lollipop.windowslauncher.databinding.FragmentDesktopBinding
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileAdapter
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.impl.AppTile
import com.lollipop.windowslauncher.utils.*
import com.lollipop.windowslauncher.views.TileLayoutManager
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author lollipop
 * @date 1/30/21 14:01
 * 桌面的碎片
 */
class DesktopFragment : BaseFragment() {

    private val viewBinding: FragmentDesktopBinding by lazyBind()

    private val tileList = ArrayList<Tile>()

    private val appHelper = IconHelper.newHelper { null }

    private val insetsHelper = TileInsetsHelper(0, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate")

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appHelper.loadAppInfo(context)
        val tileSizeValues = TileSize.values()
        val random = Random()
        val appCount = appHelper.appCount.range(0, 30)
        log("appCount = $appCount")
        for (i in 0 until appCount) {
            val appInfo = appHelper.getAppInfo(i)
            tileList.add(AppTile(appInfo).apply {
                size = tileSizeValues[random.nextInt(tileSizeValues.size)]
            })
        }
        viewBinding.tileGroup.adapter?.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.tileGroup.apply {
            val tileCol = LSettings.getTileCol(context)
            insetsHelper.spanCount = tileCol
            insetsHelper.space = LSettings.getCreviceMode(context).dp.dp2px().toInt()
            layoutManager = TileLayoutManager(
                tileCol,
                {
                    tileList[it].size
                },
                insetsHelper
            )
            adapter = TileAdapter(tileList, ::onTileClick, ::onTileLongClick)
        }
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.tileGroup.adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        context?.let {
        }
    }

    private fun onTileClick(tile: Tile) {

    }

    private fun onTileLongClick(tile: Tile) {
    }

    private class TileInsetsHelper(
        var space: Int,
        var spanCount: Int
    ) : TileLayoutManager.InsetsProvider {

        override fun getInsets(
            insets: Rect,
            x: Int,
            y: Int,
            size: TileSize,
            orientation: TileLayoutManager.Orientation
        ) {
            val half = space * 0.5F
            val halfUp = (half + 0.5F).toInt()
            val halfDown = half.toInt()
            val left = if (x == 0) {
                space
            } else {
                halfDown
            }
            val top = if (y == 0) {
                space
            } else {
                halfDown
            }
            val right = if (orientation.isVertical) {
                if (x + size.width == spanCount) {
                    space
                } else {
                    halfUp
                }
            } else {
                halfUp
            }
            val bottom = if (orientation.isVertical) {
                halfUp
            } else {
                if (y + size.height == spanCount) {
                    space
                } else {
                    halfUp
                }
            }
            insets.set(left, top, right, bottom)
        }

    }

}