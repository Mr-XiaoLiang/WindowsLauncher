package com.lollipop.windowslauncher.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.lollipop.windowslauncher.base.BaseFragment
import com.lollipop.windowslauncher.databinding.FragmentDesktopBinding
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.tile.TileViewCreator
import com.lollipop.windowslauncher.tile.impl.AppTile
import com.lollipop.windowslauncher.utils.*

/**
 * @author lollipop
 * @date 1/30/21 14:01
 * 桌面的碎片
 */
class DesktopFragment : BaseFragment() {

    private val viewBinding: FragmentDesktopBinding by lazyBind()

    private val tileList = ArrayList<Tile>()

    private val appHelper = IconHelper.newHelper()

    private var openAppListCallback: OpenAppListCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        identityCheck<OpenAppListCallback> {
            openAppListCallback = it
        }

        appHelper.loadAppInfo(context)
        val tileSizeValues = TileSize.values()
        val appCount = appHelper.appCount//.range(0, 30)
        for (i in 0 until appCount) {
            val appInfo = appHelper.getAppInfo(i)
            tileList.add(AppTile(appInfo).apply {
                size = tileSizeValues[i % tileSizeValues.size]
            })
        }
    }

    override fun onDetach() {
        super.onDetach()
        openAppListCallback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.appListBtn.setOnClickListener {
            openAppListCallback?.openAppList()
        }

        viewBinding.loadingView.apply {
            defLineStyle()
            post {
                show()
            }
        }
        viewBinding.appListBtn.tryInvisible()
        val myContext = context
        if (myContext == null) {
            initTileGroup()
        } else {
            doAsync {
                for (tile in tileList) {
                    tile.loadSync(myContext)
                }
                onUI {
                    viewBinding.loadingView.hide {
                        initTileGroup()
                    }
                }
            }
        }
    }

    private fun initTileGroup() {
        viewBinding.tileGroup.apply {
            space = LSettings.getCreviceMode(context).dp.dp2px().toInt()
            spanCount = LSettings.getTileCol(context)
            bindCreator(TileViewCreator())
            addTile(tileList)
        }
        viewBinding.appListBtn.tryVisible()
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.tileGroup.notifyTileChanged()
        viewBinding.loadingView.pointColor = LColor.primary
        viewBinding.appListBtn.updateColor()
    }

    override fun onInsetsChange(root: View, left: Int, top: Int, right: Int, bottom: Int) {
        super.onInsetsChange(root, left, top, right, bottom)
        viewBinding.tileGroup.apply {
            setViewInsets(left, top, right, bottom)
        }
    }

    override fun onResume() {
        super.onResume()
        viewBinding.tileGroup.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewBinding.tileGroup.onPause()
    }

    interface OpenAppListCallback {
        fun openAppList()
    }

}