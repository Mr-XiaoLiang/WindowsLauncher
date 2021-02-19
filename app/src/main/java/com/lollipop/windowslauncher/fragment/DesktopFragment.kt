package com.lollipop.windowslauncher.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lollipop.windowslauncher.base.BaseFragment
import com.lollipop.windowslauncher.databinding.FragmentDesktopBinding
import com.lollipop.windowslauncher.databinding.ItemTileAppBinding
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.TileViewCreator
import com.lollipop.windowslauncher.tile.impl.AppTile
import com.lollipop.windowslauncher.tile.tileView.AppTileView
import com.lollipop.windowslauncher.utils.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appHelper.loadAppInfo(context)
        val tileSizeValues = TileSize.values()
        val appCount = appHelper.appCount.range(0, 30)
        log("appCount = $appCount")
        for (i in 0 until appCount) {
            val appInfo = appHelper.getAppInfo(i)
            tileList.add(AppTile(appInfo).apply {
                size = tileSizeValues[i % tileSizeValues.size]
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.tileGroup.apply {
            space = LSettings.getCreviceMode(context).dp.dp2px().toInt()
            spanCount = LSettings.getTileCol(context)
            bindCreator(TileViewCreator())
            delay(1000L) {
                addTile(tileList)
            }
        }
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.tileGroup.notifyTileChanged()
    }

    override fun onInsetsChange(root: View, left: Int, top: Int, right: Int, bottom: Int) {
        super.onInsetsChange(root, left, top, right, bottom)
    }

    override fun onResume() {
        super.onResume()
        viewBinding.tileGroup.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewBinding.tileGroup.onPause()
    }

}