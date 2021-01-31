package com.lollipop.windowslauncher.fragment

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
import com.lollipop.windowslauncher.utils.DefaultItemDecoration
import com.lollipop.windowslauncher.utils.lazyBind

/**
 * @author lollipop
 * @date 1/30/21 14:01
 * 桌面的碎片
 */
class DesktopFragment: BaseFragment() {

    private val viewBinding: FragmentDesktopBinding by lazyBind()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onColorChanged() {
        super.onColorChanged()
    }

    private class SpanSizeController: GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            TODO("Not yet implemented")
        }
    }

    private class TileDecoration(space: Int): DefaultItemDecoration(space) {

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