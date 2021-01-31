package com.lollipop.windowslauncher.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * @author lollipop
 * @date 1/23/21 16:53
 */
open class DefaultItemDecoration(
    protected val space: Int,
    protected var top: Int = 0,
    protected var bottom: Int = 0,
) : RecyclerView.ItemDecoration() {

    fun setHeader(top: Int) {
        this.top = top
    }

    fun setFooter(bottom: Int) {
        this.bottom = bottom
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        when (parent.layoutManager) {
            is LinearLayoutManager -> {
                getItemOffsetsByLinearLayout(outRect, view, parent)
            }
            is GridLayoutManager -> {
                getItemOffsetsByGridLayout(outRect, view, parent)
            }
            is StaggeredGridLayoutManager -> {
                getItemOffsetsByStaggeredGridLayout(outRect, view, parent)
            }
        }
    }

    private fun getItemOffsetsByLinearLayout(
            outRect: Rect, view: View, parent: RecyclerView) {
        val layoutManager = parent.layoutManager as LinearLayoutManager

        outRect.left = space
        outRect.right = space
        outRect.top = space / 2
        outRect.bottom = space / 2

        val position = parent.getChildAdapterPosition(view)
        updateHeaderFooter(parent, position, outRect, 1)

        if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
            val top = outRect.left
            val bottom = outRect.right
            val left = outRect.top
            val right = outRect.bottom
            outRect.set(left, top, right, bottom)
        }
    }

    private fun getItemOffsetsByGridLayout(
            outRect: Rect, view: View, parent: RecyclerView) {

        val layoutManager = parent.layoutManager as GridLayoutManager
        val spanCount = layoutManager.spanCount
        val params = view.layoutParams
        if (params is GridLayoutManager.LayoutParams) {
            when {
                params.spanIndex % spanCount == 0 -> {
                    // 左边
                    outRect.left = space
                    outRect.right = space / 2
                }
                params.spanIndex % spanCount == spanCount - 1 -> {
                    // 右边
                    outRect.left = space / 2
                    outRect.right = space
                }
                else -> {
                    // 中间
                    outRect.left = space / 2
                    outRect.right = space / 2
                }
            }
        }
        outRect.top = space / 2
        outRect.bottom = space / 2

        val position = parent.getChildAdapterPosition(view)
        updateHeaderFooter(parent, position, outRect, spanCount)

        if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
            val top = outRect.left
            val bottom = outRect.right
            val left = outRect.top
            val right = outRect.bottom
            outRect.set(left, top, right, bottom)
        }

    }

    private fun getItemOffsetsByStaggeredGridLayout(
            outRect: Rect, view: View, parent: RecyclerView) {
        val layoutManager = parent.layoutManager as StaggeredGridLayoutManager
        val spanCount = layoutManager.spanCount
        val params = view.layoutParams
        if (params is StaggeredGridLayoutManager.LayoutParams) {
            when {
                params.spanIndex % spanCount == 0 -> {
                    // 左边
                    outRect.left = space
                    outRect.right = space / 2
                }
                params.spanIndex % spanCount == spanCount - 1 -> {
                    // 右边
                    outRect.left = space / 2
                    outRect.right = space
                }
                else -> {
                    // 中间
                    outRect.left = space / 2
                    outRect.right = space / 2
                }
            }
        }
        outRect.top = space / 2
        outRect.bottom = space / 2

        val position = parent.getChildAdapterPosition(view)
        updateHeaderFooter(parent, position, outRect, spanCount)

        if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
            val top = outRect.left
            val bottom = outRect.right
            val left = outRect.top
            val right = outRect.bottom
            outRect.set(left, top, right, bottom)
        }
    }

    private fun updateHeaderFooter(
        parent: RecyclerView,
        position: Int,
        outRect: Rect,
        spanCount: Int,
    ) {
        if (position / spanCount == 0) {
            outRect.top = if (top != 0) {
                top
            } else {
                space
            }
        }
        val itemCount = parent.adapter?.itemCount?:return
        if (position / spanCount == itemCount / spanCount) {
            outRect.bottom = if (bottom != 0) {
                bottom
            } else {
                space
            }
        }
    }

}