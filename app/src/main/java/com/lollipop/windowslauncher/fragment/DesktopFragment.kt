package com.lollipop.windowslauncher.fragment

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.iconcore.ui.BaseFragment
import com.lollipop.windowslauncher.databinding.FragmentDesktopBinding
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileAdapter
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.impl.AppTile
import com.lollipop.windowslauncher.utils.*
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

//    private val tileDecoration = TileDecoration(0)

    private val appHelper = IconHelper.newHelper { null }

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
//            layoutManager = GridLayoutManager(
//                context, LSettings.getTileCol(context),
//                RecyclerView.VERTICAL, false).apply {
//                    spanSizeLookup = SpanSizeController(tileList)
//            }
//            layoutManager = TileLayout(
//                LSettings.getTileCol(context),
//                orientation = RecyclerView.VERTICAL,
//                infoProvider = TileLayoutController(
//                    LSettings.getCreviceMode(context).dp.dp2px().toInt(),
//                    tileList
//                )
//            )
//            addItemDecoration(tileDecoration)
            adapter = TileAdapter(tileList, ::onTileClick, ::onTileLongClick)
        }
    }

    override fun onColorChanged() {
        super.onColorChanged()
//        tileDecoration.color = LColor.background
    }

    override fun onResume() {
        super.onResume()
        context?.let {
//            tileDecoration.setSpace(LSettings.getCreviceMode(it).dp.dp2px().toInt()) {
//                viewBinding.tileGroup.requestLayout()
//            }
        }
    }

    private fun onTileClick(tile: Tile) {
    }

    private fun onTileLongClick(tile: Tile) {
    }

    private class SpanSizeController(
        private val tileList: ArrayList<Tile>
    ) : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return tileList[position].size.width
        }
    }

    private class TileDecoration(space: Int) : DefaultItemDecoration(space) {

        private val tileBounds = Rect()
        private val tileOffset = Rect()

        private val paint = Paint().apply {
            isAntiAlias = true
        }

        var color: Int
            get() {
                return paint.color
            }
            set(value) {
                paint.color = value
            }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                parent.getDecoratedBoundsWithMargins(child, tileBounds)
                getItemOffsets(tileOffset, child, parent, state)
                c.save()
                c.clipRect(tileBounds)
                c.drawRect(tileOffset, paint)
                c.restore()
            }
        }
    }

}